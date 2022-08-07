package com.illtamer.infinite.bot.expansion.chat.util;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AtUtil {


    public static void one(Player target, String sender) {
        target.sendTitle(ChatColor.AQUA + "有人@你", ChatColor.AQUA + sender + ChatColor.AQUA + " @了你");
        target.sendMessage(ChatColor.AQUA + "[@] " + ChatColor.YELLOW + ">>> " + ChatColor.GREEN + sender + ChatColor.AQUA + " @了你");
        playSound(1, target);
    }

    public static void all(Player target, String sender) {
        target.sendTitle(ChatColor.AQUA + "有全体消息", ChatColor.AQUA + sender + ChatColor.AQUA + " @了全体成员");
        target.sendMessage(ChatColor.AQUA + "[@] " + ChatColor.YELLOW + ">>> " + ChatColor.GREEN + sender + ChatColor.AQUA + " @了你");
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
