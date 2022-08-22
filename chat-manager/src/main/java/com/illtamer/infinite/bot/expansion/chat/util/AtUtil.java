package com.illtamer.infinite.bot.expansion.chat.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AtUtil {

    public static void one(String title1, String title2, String message, Player target) {
        target.sendTitle(title1, title2);
        target.sendMessage(message);
        playSound(1, target);
    }

    public static void all(String title1, String title2, String message, Player target) {
        target.sendTitle(title1, title2);
        target.sendMessage(message);
        playSound(1, target);
    }

    public static void playSound(int sound, Player target) {
        switch(sound) {
            case 1:
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                break;
            case 2:
                target.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
                break;
            case 3:
                target.playSound(target.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0F, 1.0F);
                break;
            case 4:
                target.playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0F, 1.0F);
                break;
        }
    }

}
