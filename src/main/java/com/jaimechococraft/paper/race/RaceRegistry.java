package com.jaimechococraft.paper.race;

import com.magmaguy.freeminecraftmodels.api.ModeledEntityManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Carga las razas definidas en config.yml y ofrece seleccion aleatoria ponderada
 * y herencia simple para la cria.
 */
public class RaceRegistry {

    private final Plugin plugin;
    private final Map<String, ChocoboRace> races = new LinkedHashMap<>();
    private final Random random = new Random();

    public RaceRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Recarga las razas desde el config.yml actual del plugin.
     * Las razas cuyo modelo adulto no existe en FMM se descartan (con aviso en consola),
     * para no intentar spawnear un modelo inexistente.
     */
    public void reload() {
        races.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("races");
        if (root == null) {
            plugin.getLogger().warning("No se encontro la seccion 'races' en config.yml");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            String displayName = section.getString("display-name", id);
            String modelAdult = section.getString("model-adult", "");
            String modelBaby = section.getString("model-baby", modelAdult);
            double speed = section.getDouble("movement-speed", 0.335);
            double jump = section.getDouble("jump-strength", 0.7);
            int weight = Math.max(1, section.getInt("weight", 10));

            boolean modelExists;
            try {
                modelExists = ModeledEntityManager.modelExists(modelAdult);
            } catch (Throwable t) {
                // Si por lo que sea la llamada a FMM falla (version distinta, no cargado aun, etc.)
                // no tumbamos el arranque del plugin: solo avisamos y seguimos.
                plugin.getLogger().warning("No se pudo verificar el modelo '" + modelAdult
                        + "' en FreeMinecraftModels: " + t.getMessage());
                modelExists = true;
            }

            if (!modelExists) {
                plugin.getLogger().warning("Raza '" + id + "' deshabilitada: el modelo FMM '"
                        + modelAdult + "' no existe todavia. Crea/instala ese .bbmodel en FMM"
                        + " o cambia 'model-adult' en config.yml.");
                continue;
            }

            races.put(id, new ChocoboRace(id, displayName, modelAdult, modelBaby, speed, jump, weight));
        }

        plugin.getLogger().info("Razas de chocobo cargadas: " + races.keySet());
    }

    public ChocoboRace get(String id) {
        return races.get(id.toLowerCase());
    }

    public boolean has(String id) {
        return races.containsKey(id.toLowerCase());
    }

    public List<ChocoboRace> all() {
        return new ArrayList<>(races.values());
    }

    /** Elige una raza al azar respetando los pesos configurados. */
    public ChocoboRace randomWeighted() {
        List<ChocoboRace> values = all();
        if (values.isEmpty()) return null;

        int totalWeight = values.stream().mapToInt(ChocoboRace::getWeight).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        int cursor = 0;
        for (ChocoboRace race : values) {
            cursor += race.getWeight();
            if (roll < cursor) return race;
        }
        return values.get(values.size() - 1);
    }

    /**
     * Herencia simple: 80% de las veces la cria hereda la raza de uno de los dos padres
     * (al azar), 20% de las veces "muta" a una raza aleatoria ponderada global
     * (asi las razas raras como el dorado pueden salir incluso de padres comunes).
     */
    public ChocoboRace inherit(ChocoboRace mother, ChocoboRace father) {
        if (mother == null && father == null) return randomWeighted();
        if (mother == null) return father;
        if (father == null) return mother;

        if (random.nextDouble() < 0.2) {
            ChocoboRace mutation = randomWeighted();
            if (mutation != null) return mutation;
        }
        return random.nextBoolean() ? mother : father;
    }
}
