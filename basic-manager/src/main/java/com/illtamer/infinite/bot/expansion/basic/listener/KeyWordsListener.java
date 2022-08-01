package com.illtamer.infinite.bot.expansion.basic.listener;

import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

public class KeyWordsListener implements Listener {

    @EventHandler
    public void onDailyPlayer(MessageEvent event) {
        if (!StaticAPI.isAdmin(event.getUserId()) || !"今日新玩家".equals(event.getRawMessage())) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> {
            int count = 0;
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (equals(new Date(System.currentTimeMillis()),new Date(player.getFirstPlayed()))) {
                    count ++;
                }
            }
            event.reply(String.format("今日新玩家数量: %s 人", count));
        });
    }

    @EventHandler
    public void onCheckBind(MessageEvent event) {
        if (!"我的绑定".equals(event.getRawMessage())) {
            return;
        }
        PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getUuid() == null) {
            event.reply("暂未查询到您的信息，请绑定后重试");
        } else {
            OfflinePlayer player = data.getOfflinePlayer();
            event.reply(String.format(
                    "绑定的玩家ID: %s|%s",
                    player.getName(),
                    player.isOnline() ? "(在线)" : ("(离线)\n最后一次登录: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(player.getLastPlayed())))
            ));
        }
    }

    @EventHandler
    public void onLoginOut(MessageEvent event) {
        if (!"强制下线".equals(event.getRawMessage())) {
            return;
        }
        PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getUuid() == null) {
            event.reply("暂未查询到您的信息，请绑定后重试");
        } else {
            final Player onlinePlayer = Lambda.nullableInvoke(OfflinePlayer::getPlayer, data.getOfflinePlayer());
            if (onlinePlayer != null) {
                Bukkit.getScheduler().runTask(Bootstrap.getInstance(), () -> onlinePlayer.kickPlayer(PluginUtil.parseColor("&c您已被(QQ: " + event.getSender().getUserId() + ")强制下线")));
                event.reply("强制下线成功");
            } else {
                event.reply("您的账号并未在线");
            }
        }
    }

    @EventHandler
    public void onShowPlayers(GroupMessageEvent event) {
        if (!"服务器在线".equals(event.getRawMessage())) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> {
            Collection<? extends Player> players = Bootstrap.getInstance().getServer().getOnlinePlayers();
            Iterator<? extends Player> iterator = players.iterator();
            if (!iterator.hasNext()) {
                event.reply("当前无玩家在线！");
                return;
            }
            String opPrefix = "\n管理员：", playerPrefix = "\n玩家：";
            StringBuilder opList = new StringBuilder(opPrefix);
            StringBuilder playerList = new StringBuilder(playerPrefix);
            while (iterator.hasNext()) {
                Player player = iterator.next();
                if (player.isOp()) {
                    opList.append(PluginUtil.clearColor(player.getDisplayName())).append(", ");
                } else {
                    playerList.append(PluginUtil.clearColor(player.getDisplayName())).append(", ");
                }
            }
            String format = String.format("服务器当前总人数：%d%s%s",
                    players.size(),
                    opList.length() == opPrefix.length() ? "" : opList.toString(),
                    playerList.length() == playerPrefix.length() ? "" : playerList.toString());
            event.reply(format);
        });
    }

    private static boolean equals(Date date1, Date date2) {
            Calendar c1 = Calendar.getInstance();
            c1.setTime(date1);
            Calendar c2 = Calendar.getInstance();
            c2.setTime(date2);
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}
