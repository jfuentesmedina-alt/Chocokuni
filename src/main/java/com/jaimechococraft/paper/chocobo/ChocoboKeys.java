package com.jaimechococraft.paper.chocobo;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Horse;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Centraliza las NamespacedKey usadas para marcar un Horse vanilla como "chocobo"
 * y guardar sus metadatos (raza, etapa, dueno) en su PersistentDataContainer.
 */
public class ChocoboKeys {

    public static final String STAGE_BABY = "BABY";
    public static final String STAGE_ADULT = "ADULT";

    private final NamespacedKey isChocobo;
    private final NamespacedKey raceId;
    private final NamespacedKey stage;
    private final NamespacedKey ownerUuid;
    private final NamespacedKey spawnEpochMillis;

    public ChocoboKeys(Plugin plugin) {
        this.isChocobo = new NamespacedKey(plugin, "is_chocobo");
        this.raceId = new NamespacedKey(plugin, "race_id");
        this.stage = new NamespacedKey(plugin, "stage");
        this.ownerUuid = new NamespacedKey(plugin, "owner_uuid");
        this.spawnEpochMillis = new NamespacedKey(plugin, "spawn_epoch_millis");
    }

    public boolean isChocobo(Horse horse) {
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        return pdc.has(isChocobo, PersistentDataType.BYTE);
    }

    public void markChocobo(Horse horse) {
        horse.getPersistentDataContainer().set(isChocobo, PersistentDataType.BYTE, (byte) 1);
    }

    public String getRaceId(Horse horse) {
        return horse.getPersistentDataContainer().get(raceId, PersistentDataType.STRING);
    }

    public void setRaceId(Horse horse, String id) {
        horse.getPersistentDataContainer().set(raceId, PersistentDataType.STRING, id);
    }

    public String getStage(Horse horse) {
        return horse.getPersistentDataContainer().getOrDefault(stage, PersistentDataType.STRING, STAGE_ADULT);
    }

    public void setStage(Horse horse, String value) {
        horse.getPersistentDataContainer().set(stage, PersistentDataType.STRING, value);
    }

    public UUID getOwner(Horse horse) {
        String raw = horse.getPersistentDataContainer().get(ownerUuid, PersistentDataType.STRING);
        return raw == null ? null : UUID.fromString(raw);
    }

    public void setOwner(Horse horse, UUID uuid) {
        horse.getPersistentDataContainer().set(ownerUuid, PersistentDataType.STRING, uuid.toString());
    }

    public void setSpawnEpochMillis(Horse horse, long millis) {
        horse.getPersistentDataContainer().set(spawnEpochMillis, PersistentDataType.LONG, millis);
    }

    public long getSpawnEpochMillis(Horse horse) {
        return horse.getPersistentDataContainer().getOrDefault(spawnEpochMillis, PersistentDataType.LONG, 0L);
    }
}
