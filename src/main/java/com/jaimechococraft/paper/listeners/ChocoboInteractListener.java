package com.jaimechococraft.paper.listeners;

import com.jaimechococraft.paper.chocobo.ChocoboKeys;
import com.jaimechococraft.paper.chocobo.ChocoboManager;
import com.jaimechococraft.paper.items.ChocoboItems;
import com.jaimechococraft.paper.race.ChocoboRace;
import com.jaimechococraft.paper.race.RaceRegistry;
import com.jaimechococraft.paper.util.Msg;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Random;
import java.util.UUID;

public class ChocoboInteractListener implements Listener {

    private final Plugin plugin;
    private final ChocoboManager manager;
    private final ChocoboKeys keys;
    private final ChocoboItems items;
    private final RaceRegistry raceRegistry;
    private final Random random = new Random();

    public ChocoboInteractListener(Plugin plugin, ChocoboManager manager, ChocoboItems items, RaceRegistry raceRegistry) {
        this.plugin = plugin;
        this.manager = manager;
        this.keys = manager.keys();
        this.items = items;
        this.raceRegistry = raceRegistry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractHorse(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Horse horse)) return;
        if (!keys.isChocobo(horse)) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        // 1) Doma con Gysahl Greens
        if (!horse.isTamed() && items.isGysahlGreens(inHand)) {
            event.setCancelled(true);
            consumeOne(player, inHand);

            double chance = plugin.getConfig().getDouble("settings.taming-chance", 0.35);
            if (random.nextDouble() <= chance) {
                horse.setTamed(true);
                horse.setOwner(player);
                keys.setOwner(horse, player.getUniqueId());
                Msg.send(player, "tamed");
            } else {
                Msg.send(player, "taming-failed");
            }
            return;
        }

        if (horse.isTamed() && items.isGysahlGreens(inHand)) {
            UUID owner = keys.getOwner(horse);
            boolean allowOthers = plugin.getConfig().getBoolean("settings.allow-riding-by-non-owners", false);
            boolean isOwner = owner != null && owner.equals(player.getUniqueId());

            if (!isOwner && !allowOthers && !player.isOp()) {
                event.setCancelled(true);
                Msg.send(player, "not-owner");
                return;
            }

            // 2) Gysahl Greens en un adulto domado -> modo amor (cria)
            if (manager.isAdult(horse) && !horse.isLoveMode()) {
                event.setCancelled(true);
                consumeOne(player, inHand);
                int loveTicks = plugin.getConfig().getInt("settings.love-mode-ticks", 600);
                horse.setLoveModeTicks(loveTicks);
                Msg.send(player, "love-mode");
                return;
            }
        }

        // 3) Bloquear montar sin silla, para que la regla sea clara para el jugador
        if (horse.isTamed() && (horse.getInventory().getSaddle() == null
                || horse.getInventory().getSaddle().getType().isAir())) {
            event.setCancelled(true);
            Msg.send(player, "need-saddle");
            return;
        }

        // 4) Chequeo de dueno para montar
        if (horse.isTamed()) {
            UUID owner = keys.getOwner(horse);
            boolean allowOthers = plugin.getConfig().getBoolean("settings.allow-riding-by-non-owners", false);
            boolean isOwner = owner != null && owner.equals(player.getUniqueId());
            if (!isOwner && !allowOthers && !player.isOp()) {
                event.setCancelled(true);
                Msg.send(player, "not-owner");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUseEgg(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack inHand = event.getItem();
        if (!items.isChocoboEgg(inHand)) return;
        if (event.getClickedBlock() == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        ChocoboRace race = raceRegistry.randomWeighted();
        if (race == null) {
            Msg.send(player, "race-unavailable", "%race%", "?");
            return;
        }

        consumeOne(player, inHand);
        var spawnLocation = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);
        manager.spawnChocobo(spawnLocation, race, ChocoboKeys.STAGE_BABY, player.getUniqueId(), horse -> {
            if (horse != null) {
                Msg.send(player, "egg-hatched");
            }
        });
    }

    private void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }
}
