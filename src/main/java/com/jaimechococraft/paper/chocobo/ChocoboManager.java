package com.jaimechococraft.paper.chocobo;

import com.jaimechococraft.paper.race.ChocoboRace;
import com.jaimechococraft.paper.race.RaceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Punto central para todo lo que involucra el ciclo de vida de un chocobo:
 * pedirle a FMM que "vista" un Horse vanilla con el modelo correspondiente,
 * etiquetar ese Horse con los metadatos de ChocoCraft, aplicarle atributos
 * segun su raza y gestionar el crecimiento de bebe a adulto.
 *
 * NOTA IMPORTANTE:
 * FreeMinecraftModels no expone una API publica para "spawnear el modelo X
 * en la entidad Y". Lo unico soportado publicamente es su propio comando
 * (configurable en config.yml -> settings.spawn-command). Por eso, spawnear
 * consiste en: 1) pedirle a FMM por comando que cree el modelo, 2) al tick
 * siguiente buscar el Horse vanilla recien aparecido cerca del punto de spawn
 * y "etiquetarlo" como chocobo. Si tu version de FMM usa otra sintaxis de
 * comando, ajusta settings.spawn-command en config.yml.
 */
public class ChocoboManager {

    private final Plugin plugin;
    private final RaceRegistry raceRegistry;
    private final ChocoboKeys keys;

    public ChocoboManager(Plugin plugin, RaceRegistry raceRegistry, ChocoboKeys keys) {
        this.plugin = plugin;
        this.raceRegistry = raceRegistry;
        this.keys = keys;
    }

    public ChocoboKeys keys() {
        return keys;
    }

    /**
     * Pide a FMM spawnear el modelo de la raza indicada en la ubicacion dada,
     * y una vez aparece el Horse subyacente lo etiqueta como chocobo.
     *
     * @param location ubicacion de spawn
     * @param race     raza a spawnear
     * @param stage    ChocoboKeys.STAGE_BABY o STAGE_ADULT
     * @param owner    dueno inicial (puede ser null para chocobos salvajes)
     * @param callback opcional: recibe el Horse ya etiquetado (o null si no se encontro)
     */
    public void spawnChocobo(Location location, ChocoboRace race, String stage, UUID owner, Consumer<Horse> callback) {
        boolean adult = ChocoboKeys.STAGE_ADULT.equals(stage);
        String model = race.getModel(adult);

        String template = plugin.getConfig().getString("settings.spawn-command",
                "fmm spawn %model% %world% %x% %y% %z%");
        String command = template
                .replace("%model%", model)
                .replace("%world%", location.getWorld().getName())
                .replace("%x%", String.valueOf(location.getX()))
                .replace("%y%", String.valueOf(location.getY()))
                .replace("%z%", String.valueOf(location.getZ()));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        double radius = plugin.getConfig().getDouble("settings.spawn-search-radius", 4.0);

        // Le damos un tick a FMM para crear la entidad antes de buscarla.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Horse found = findFreshHorse(location, radius);
            if (found == null) {
                plugin.getLogger().warning("Se ejecuto '" + command + "' pero no se encontro"
                        + " ningun Horse nuevo cerca. Revisa que el modelo '" + model
                        + "' exista en FMM y que la sintaxis del comando sea correcta.");
                if (callback != null) callback.accept(null);
                return;
            }

            keys.markChocobo(found);
            keys.setRaceId(found, race.getId());
            keys.setStage(found, stage);
            keys.setSpawnEpochMillis(found, System.currentTimeMillis());
            if (owner != null) {
                keys.setOwner(found, owner);
                found.setTamed(true);
                if (Bukkit.getOfflinePlayer(owner).isOnline()) {
                    found.setOwner((org.bukkit.entity.AnimalTamer) Bukkit.getPlayer(owner));
                }
            }

            applyRaceAttributes(found, race);

            if (!adult) {
                // Escala el tiempo de crecimiento vanilla al configurado en minutes.
                int minutes = plugin.getConfig().getInt("settings.baby-growth-time-minutes", 20);
                found.setAge(-minutes * 60 * 20); // ticks negativos = tiempo restante como bebe
                found.setAgeLock(false);
            } else {
                found.setAdult();
            }

            if (callback != null) callback.accept(found);
        });
    }

    /** Busca el Horse mas nuevo (sin etiqueta de chocobo aun) cerca de una ubicacion. */
    private Horse findFreshHorse(Location location, double radius) {
        Collection<Entity> nearby = location.getWorld().getNearbyEntities(location, radius, radius, radius);
        Horse candidate = null;
        for (Entity entity : nearby) {
            if (entity.getType() != EntityType.HORSE) continue;
            Horse horse = (Horse) entity;
            if (keys.isChocobo(horse)) continue;
            candidate = horse;
        }
        return candidate;
    }

    /** Aplica velocidad de movimiento y fuerza de salto segun la raza. */
    public void applyRaceAttributes(Horse horse, ChocoboRace race) {
        AttributeInstance speed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(race.getMovementSpeed());

        AttributeInstance jump = horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH);
        if (jump != null) jump.setBaseValue(race.getJumpStrength());
        // Nota: en Paper 1.21+ los nombres de Attribute pueden variar (p.ej. sin el
        // prefijo GENERIC_). Si esto no compila contra tu version exacta de la API,
        // revisa la clase org.bukkit.attribute.Attribute de tu paper-api y ajusta
        // estas dos constantes.
    }

    public ChocoboRace getRace(Horse horse) {
        String id = keys.getRaceId(horse);
        return id == null ? null : raceRegistry.get(id);
    }

    public boolean isAdult(Horse horse) {
        return ChocoboKeys.STAGE_ADULT.equals(keys.getStage(horse));
    }

    /**
     * Cuando un bebe chocobo termina de crecer (vanilla ya lo puso adulto),
     * lo reemplazamos por la version adulta del modelo: no existe una API
     * publica para "cambiarle el modelo" a una entidad ya spawneada en FMM,
     * asi que la forma fiable de lograrlo es remover el Horse bebe y volver
     * a spawnear el chocobo adulto en el mismo punto, conservando raza y dueno.
     */
    public void growUp(Horse babyHorse) {
        ChocoboRace race = getRace(babyHorse);
        if (race == null) return;

        UUID owner = keys.getOwner(babyHorse);
        Location location = babyHorse.getLocation();
        boolean wasTamed = babyHorse.isTamed();

        babyHorse.remove();

        spawnChocobo(location, race, ChocoboKeys.STAGE_ADULT, owner, grown -> {
            if (grown != null && wasTamed) {
                grown.setTamed(true);
            }
        });
    }
}
