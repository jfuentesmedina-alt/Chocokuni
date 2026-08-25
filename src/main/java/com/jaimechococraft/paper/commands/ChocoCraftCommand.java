package com.jaimechococraft.paper.commands;

import com.jaimechococraft.paper.chocobo.ChocoboKeys;
import com.jaimechococraft.paper.chocobo.ChocoboManager;
import com.jaimechococraft.paper.items.ChocoboItems;
import com.jaimechococraft.paper.race.ChocoboRace;
import com.jaimechococraft.paper.race.RaceRegistry;
import com.jaimechococraft.paper.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChocoCraftCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final ChocoboManager manager;
    private final RaceRegistry raceRegistry;
    private final ChocoboItems items;

    public ChocoCraftCommand(Plugin plugin, ChocoboManager manager, RaceRegistry raceRegistry, ChocoboItems items) {
        this.plugin = plugin;
        this.manager = manager;
        this.raceRegistry = raceRegistry;
        this.items = items;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Msg.color("&e/chococraft <spawn|give|races|reload>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "races" -> {
                List<String> names = raceRegistry.all().stream()
                        .map(r -> Msg.color(r.getDisplayName()) + " &7(" + r.getId() + ")")
                        .collect(Collectors.toList());
                sender.sendMessage(Msg.color("&6Razas disponibles:"));
                names.forEach(sender::sendMessage);
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("chococraft.admin")) {
                    Msg.send(sender, "no-permission");
                    return true;
                }
                plugin.reloadConfig();
                raceRegistry.reload();
                Msg.send(sender, "reloaded");
                return true;
            }
            case "spawn" -> {
                if (!sender.hasPermission("chococraft.admin")) {
                    Msg.send(sender, "no-permission");
                    return true;
                }
                return handleSpawn(sender, args);
            }
            case "give" -> {
                if (!sender.hasPermission("chococraft.admin")) {
                    Msg.send(sender, "no-permission");
                    return true;
                }
                return handleGive(sender, args);
            }
            default -> {
                sender.sendMessage(Msg.color("&e/chococraft <spawn|give|races|reload>"));
                return true;
            }
        }
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Msg.color("&eUso: /chococraft spawn <raza> [jugador]"));
            return true;
        }

        ChocoboRace race = raceRegistry.get(args[1]);
        if (race == null) {
            Msg.send(sender, "unknown-race", "%race%", args[1]);
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(Msg.color("&cJugador no encontrado: " + args[2]));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Msg.color("&cDesde consola debes indicar un jugador."));
            return true;
        }

        manager.spawnChocobo(target.getLocation(), race, ChocoboKeys.STAGE_ADULT, target.getUniqueId(), horse -> {
            if (horse != null) {
                Msg.send(sender, "spawned", "%race%", Msg.color(race.getDisplayName()));
            }
        });
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Msg.color("&eUso: /chococraft give <gysahl|egg> [jugador] [cantidad]"));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(Msg.color("&cJugador no encontrado: " + args[2]));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Msg.color("&cDesde consola debes indicar un jugador."));
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ignored) {
            }
        }

        var item = switch (args[1].toLowerCase()) {
            case "gysahl", "greens", "gysahl-greens" -> items.createGysahlGreens();
            case "egg", "huevo" -> items.createChocoboEgg();
            default -> null;
        };

        if (item == null) {
            sender.sendMessage(Msg.color("&eItems validos: gysahl, egg"));
            return true;
        }

        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(Msg.color("&aEntregado x" + amount + " a " + target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("spawn", "give", "races", "reload"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn")) {
                return filter(raceRegistry.all().stream().map(ChocoboRace::getId).collect(Collectors.toList()), args[1]);
            }
            if (args[0].equalsIgnoreCase("give")) {
                return filter(Arrays.asList("gysahl", "egg"), args[1]);
            }
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("give"))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
