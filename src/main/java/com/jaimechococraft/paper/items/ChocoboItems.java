package com.jaimechococraft.paper.items;

import com.jaimechococraft.paper.util.Msg;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Crea y reconoce los items custom (Gysahl Greens, Huevo de Chocobo) definidos
 * en config.yml bajo la seccion "items". Se identifican mediante un PDC tag,
 * no por nombre, para que renombrar el item en config no rompa la deteccion.
 */
public class ChocoboItems {

    private final Plugin plugin;
    private final NamespacedKey gysahlKey;
    private final NamespacedKey eggKey;

    public ChocoboItems(Plugin plugin) {
        this.plugin = plugin;
        this.gysahlKey = new NamespacedKey(plugin, "gysahl_greens");
        this.eggKey = new NamespacedKey(plugin, "chocobo_egg");
    }

    public ItemStack createGysahlGreens() {
        return build("items.gysahl-greens", gysahlKey, Material.WHEAT);
    }

    public ItemStack createChocoboEgg() {
        return build("items.chocobo-egg", eggKey, Material.TURTLE_EGG);
    }

    public boolean isGysahlGreens(ItemStack item) {
        return hasTag(item, gysahlKey);
    }

    public boolean isChocoboEgg(ItemStack item) {
        return hasTag(item, eggKey);
    }

    private ItemStack build(String configPath, NamespacedKey tagKey, Material fallback) {
        Material material = fallback;
        String matName = plugin.getConfig().getString(configPath + ".material");
        if (matName != null) {
            Material parsed = Material.matchMaterial(matName);
            if (parsed != null) material = parsed;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = plugin.getConfig().getString(configPath + ".name", "");
            if (!name.isEmpty()) {
                meta.setDisplayName(Msg.color(name));
            }

            List<String> loreLines = plugin.getConfig().getStringList(configPath + ".lore");
            if (!loreLines.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String line : loreLines) colored.add(Msg.color(line));
                meta.setLore(colored);
            }

            meta.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean hasTag(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
