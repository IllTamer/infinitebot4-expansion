package com.illtamer.infinite.bot.expansion.message.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class Placeholder {

    private static boolean enabled;

    public static boolean init() {
        return enabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public static String set(String s, OfflinePlayer player) {
        if (!enabled) return s;
        return PlaceholderAPI.setPlaceholders(player, s);
    }

    public static List<String> set(List<String> s, OfflinePlayer player) {
        if (!enabled) return s;
        return PlaceholderAPI.setPlaceholders(player, s);
    }

}
