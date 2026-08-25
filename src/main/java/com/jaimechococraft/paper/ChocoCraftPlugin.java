package com.jaimechococraft.paper;

import com.jaimechococraft.paper.chocobo.ChocoboGrowthTask;
import com.jaimechococraft.paper.chocobo.ChocoboKeys;
import com.jaimechococraft.paper.chocobo.ChocoboManager;
import com.jaimechococraft.paper.commands.ChocoCraftCommand;
import com.jaimechococraft.paper.items.ChocoboItems;
import com.jaimechococraft.paper.listeners.ChocoboBreedListener;
import com.jaimechococraft.paper.listeners.ChocoboInteractListener;
import com.jaimechococraft.paper.race.RaceRegistry;
import com.jaimechococraft.paper.util.Msg;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChocoCraftPlugin extends JavaPlugin {

    private RaceRegistry raceRegistry;
    private ChocoboManager chocoboManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Msg.init(this);

        PluginManager pm = getServer().getPluginManager();
        if (pm.getPlugin("FreeMinecraftModels") == null) {
            getLogger().severe("FreeMinecraftModels no esta instalado/activo. ChocoCraftPaper se desactiva.");
            pm.disablePlugin(this);
            return;
        }

        boolean geyser = pm.getPlugin("Geyser-Spigot") != null;
        boolean floodgate = pm.getPlugin("floodgate") != null;
        getLogger().info("Geyser detectado: " + geyser + " | Floodgate detectado: " + floodgate
                + " (los chocobos son un Horse vanilla + modelo FMM, por lo que jugadores"
                + " Java y Bedrock los ven y montan igual una vez que Geyser esta activo).");

        ChocoboKeys keys = new ChocoboKeys(this);
        raceRegistry = new RaceRegistry(this);
        raceRegistry.reload();

        chocoboManager = new ChocoboManager(this, raceRegistry, keys);
        ChocoboItems items = new ChocoboItems(this);

        pm.registerEvents(new ChocoboInteractListener(this, chocoboManager, items, raceRegistry), this);
        pm.registerEvents(new ChocoboBreedListener(this, chocoboManager, raceRegistry), this);

        ChocoCraftCommand commandHandler = new ChocoCraftCommand(this, chocoboManager, raceRegistry, items);
        var cmd = getCommand("chococraft");
        if (cmd != null) {
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        }

        long intervalTicks = getConfig().getLong("settings.growth-check-interval-seconds", 20) * 20L;
        new ChocoboGrowthTask(chocoboManager, keys).runTaskTimer(this, intervalTicks, intervalTicks);

        getLogger().info("ChocoCraftPaper habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChocoCraftPaper deshabilitado.");
    }
}
