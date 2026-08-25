package com.jaimechococraft.paper.chocobo;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Recorre periodicamente los caballos-chocobo marcados como bebe y, cuando
 * el envejecimiento vanilla ya los volvio adultos, dispara el reemplazo
 * por el modelo adulto via ChocoboManager#growUp.
 */
public class ChocoboGrowthTask extends BukkitRunnable {

    private final ChocoboManager manager;
    private final ChocoboKeys keys;

    public ChocoboGrowthTask(ChocoboManager manager, ChocoboKeys keys) {
        this.manager = manager;
        this.keys = keys;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            List<Horse> toGrow = new ArrayList<>();

            for (Entity entity : world.getEntitiesByClass(Horse.class)) {
                Horse horse = (Horse) entity;
                if (!keys.isChocobo(horse)) continue;
                if (!ChocoboKeys.STAGE_BABY.equals(keys.getStage(horse))) continue;
                if (horse.isAdult()) {
                    toGrow.add(horse);
                }
            }

            for (Horse horse : toGrow) {
                manager.growUp(horse);
            }
        }
    }

    private boolean isHorseType(Entity entity) {
        return entity.getType() == EntityType.HORSE;
    }
}
