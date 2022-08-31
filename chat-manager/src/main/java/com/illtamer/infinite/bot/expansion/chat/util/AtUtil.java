package com.illtamer.infinite.bot.expansion.chat.util;

import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AtUtil {

    private static Sound sound;

    public static void init(ExpansionConfig config) {
        sound = Sound.valueOf(config.getConfig().getString("group-to-game.at-sound"));
    }

    public static void one(String title1, String title2, String message, Player target) {
        target.sendTitle(title1, title2);
        target.sendMessage(message);
        playSound(target);
    }

    public static void all(String title1, String title2, String message, Player target) {
        target.sendTitle(title1, title2);
        target.sendMessage(message);
        playSound(target);
    }

    public static void playSound(Player target) {
        target.playSound(target.getLocation(), sound, 1.0F, 1.0F);
    }

}
