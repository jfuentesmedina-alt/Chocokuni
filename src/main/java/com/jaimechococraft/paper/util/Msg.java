package com.jaimechococraft.paper.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class Msg {

    private static Plugin plugin;

    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /** Obtiene un mensaje de config.yml (messages.<key>), coloreado y con prefijo. */
    public static String get(String key) {
        String raw = plugin.getConfig().getString("messages." + key, key);
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return color(prefix + raw);
    }

    public static String get(String key, String placeholder, String value) {
        return get(key).replace(placeholder, value);
    }

    public static void send(CommandSender target, String key) {
        target.sendMessage(get(key));
    }

    public static void send(CommandSender target, String key, String placeholder, String value) {
        target.sendMessage(get(key, placeholder, value));
    }
}
