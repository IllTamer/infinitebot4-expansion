package com.illtamer.infinite.bot.expansion.message.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class Placeholder {

    private static boolean enabled;
    private static boolean deprecated;

    public static boolean init() {
        if (enabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            final String version = Bukkit.getPluginManager().getPlugin("PlaceholderAPI").getDescription().getVersion();
            final int v = Integer.parseInt(version.substring(0, version.lastIndexOf('.')).replace(".", ""));
            if (deprecated = v <= 28) {
                System.out.println("您使用的 PlaceholderAPI 版本过低，可能影响部分变量替换，请及时升级！");
            }
        }
        return enabled;
    }

    public static String set(String s, OfflinePlayer player) {
        if (!enabled) return s;
        if (deprecated) return PlaceholderAPI.setPlaceholders(player != null ? player.getPlayer() : null, s);
        return PlaceholderAPI.setPlaceholders(player, s);
    }

    public static List<String> set(List<String> s, OfflinePlayer player) {
        if (!enabled) return s;
        if (deprecated) return PlaceholderAPI.setPlaceholders(player != null ? player.getPlayer() : null, s);
        return PlaceholderAPI.setPlaceholders(player, s);
    }

}
