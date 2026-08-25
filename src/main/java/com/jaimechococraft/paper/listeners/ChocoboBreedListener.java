package com.jaimechococraft.paper.listeners;

import com.jaimechococraft.paper.chocobo.ChocoboKeys;
import com.jaimechococraft.paper.chocobo.ChocoboManager;
import com.jaimechococraft.paper.race.ChocoboRace;
import com.jaimechococraft.paper.race.RaceRegistry;
import com.jaimechococraft.paper.util.Msg;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Vanilla ya sabe criar dos Horse (crea un potrillo Horse normal). Nosotros
 * cancelamos ese potrillo "sin vestir" y en su lugar spawneamos nuestro propio
 * chocobo bebe (con el modelo FMM correcto) en el mismo punto, heredando raza
 * de los padres, y reponemos manualmente el cooldown de cria ya que al cancelar
 * el evento vanilla no lo hace por nosotros.
 */
public class ChocoboBreedListener implements Listener {

    private final Plugin plugin;
    private final ChocoboManager manager;
    private final ChocoboKeys keys;
    private final RaceRegistry raceRegistry;

    public ChocoboBreedListener(Plugin plugin, ChocoboManager manager, RaceRegistry raceRegistry) {
        this.plugin = plugin;
        this.manager = manager;
        this.keys = manager.keys();
        this.raceRegistry = raceRegistry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getMother() instanceof Horse mother) || !(event.getFather() instanceof Horse father)) return;
        if (!keys.isChocobo(mother) || !keys.isChocobo(father)) return;

        event.setCancelled(true);

        ChocoboRace motherRace = manager.getRace(mother);
        ChocoboRace fatherRace = manager.getRace(father);
        ChocoboRace childRace = raceRegistry.inherit(motherRace, fatherRace);
        if (childRace == null) return;

        UUID owner = keys.getOwner(mother) != null ? keys.getOwner(mother) : keys.getOwner(father);

        int cooldown = plugin.getConfig().getInt("settings.breed-cooldown-ticks", 6000);
        resetBreedState(mother, cooldown);
        resetBreedState(father, cooldown);

        manager.spawnChocobo(mother.getLocation(), childRace, ChocoboKeys.STAGE_BABY, owner, baby -> {
            LivingEntity breeder = event.getBreeder() instanceof Player ? (LivingEntity) event.getBreeder() : null;
            if (breeder instanceof Player player) {
                Msg.send(player, "bred");
            }
        });
    }

    private void resetBreedState(Animals animal, int cooldownTicks) {
        animal.setLoveModeTicks(0);
        animal.setBreed(false);
        animal.setAge(0);
        // setBreedCooldown existe en la interfaz Breed (Animals la implementa desde 1.19+).
        try {
            animal.setBreedCooldown(cooldownTicks);
        } catch (NoSuchMethodError ignored) {
            // API distinta en tu version: ignora el cooldown en vez de romper el plugin.
        }
    }
}
