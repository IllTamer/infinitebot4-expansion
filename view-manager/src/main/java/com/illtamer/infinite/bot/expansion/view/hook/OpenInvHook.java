package com.illtamer.infinite.bot.expansion.view.hook;

import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class OpenInvHook {

    private static boolean enable;
    private static Plugin openInv;

    public static void init(IExpansion expansion) {
        final Plugin openInv = Bukkit.getPluginManager().getPlugin("OpenInv");
        if (openInv == null) {
            enable = false;
            expansion.getLogger().warn("依赖 OpenInv 未加载，离线玩家背包信息不可用");
        } else {
            enable = true;
        }
    }

    public static Player getOfflinePlayer(OfflinePlayer offlinePlayer) {
        if (!enable) return null;
//        return ((OpenInv) openInv).loadPlayer(offlinePlayer);
        return null;
    }

}
