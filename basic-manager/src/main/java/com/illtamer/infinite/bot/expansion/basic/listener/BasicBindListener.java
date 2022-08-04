package com.illtamer.infinite.bot.expansion.basic.listener;

import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.api.event.Priority;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

public class BasicBindListener implements Listener {
    private static final HashMap<Long, BukkitTask> VERIFY = new HashMap<>();
    private static final HashMap<Player, PlayerData> BIND_DATA = new HashMap<>();
    private final long limit;

    public BasicBindListener(ExpansionConfig configFile) {
        this.limit = configFile.getConfig().getLong("bind.limit");
    }

    @EventHandler(priority = Priority.HIGHEST)
    public void onGroupBind(GroupMessageEvent event) {
        if (!StaticAPI.inGroups(event.getGroupId())) {
            return;
        }
        if (event.getRawMessage().startsWith("绑定 ")) {
            event.setCancelled(true);
            PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
            if (data != null && data.getUuid() != null) {
                event.reply("您已绑定玩家: " + Lambda.nullableInvoke(OfflinePlayer::getName, data.getOfflinePlayer()));
                return;
            }
            final PlayerData playerData = new PlayerData();
            playerData.setUserId(event.getSender().getUserId());
            bindImplement(event, playerData, limit, false);
        } else if (event.getRawMessage().startsWith("改绑 ")) {
            event.setCancelled(true);
            PlayerData playerData = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
            if (playerData == null) {
                event.reply("您未绑定过玩家");
                return;
            }
            bindImplement(event, playerData, limit, true);
        }
    }

    private static void bindImplement(GroupMessageEvent event, PlayerData data, long limit, boolean changeBind) {
        final String rawMessage = event.getRawMessage();
        if (rawMessage.length() <= 3) {
            event.reply("请使用 '绑定/改绑 玩家名称' 的格式进行更改绑定!");
            return;
        }
        String name = rawMessage.substring(3);
        Player player = Bukkit.getPlayer(name);
        if (player == null) {
            event.reply("玩家: " + name + " 未在线！");
        } else {
            BIND_DATA.put(player, data);
            final Long userId = event.getSender().getUserId();
            VERIFY.put(userId, Bukkit.getScheduler().runTaskLater(Bootstrap.getInstance(), () -> {
                final PlayerData remove = BIND_DATA.remove(player);
                VERIFY.remove(userId);
                player.sendMessage(PluginUtil.parseColor("&cQQ(&f" + remove.getUserId() + "&c)向此账号申请的绑定已过期"));
            }, limit * 60 * 20));
            event.reply("已向 " + name + " 发送申请, 请前往游戏进行验证!");
            player.sendMessage(PluginUtil.parseColor("&eQQ(&f" + userId + "&e) 正向您申请绑定, 请核实后输入 &a确认" + (changeBind ? "改绑" : "绑定") + userId + " &e完成与该账号的绑定! &7&l[五分钟内有效]"));
        }
    }

    public static class PlayerConfirmListener implements org.bukkit.event.Listener {

        @org.bukkit.event.EventHandler
        public void onConfirm(AsyncPlayerChatEvent event) {
            PlayerData data = BIND_DATA.get(event.getPlayer());
            if (data == null) return;
            long qq = data.getUserId();
            if (event.getMessage().equals("确认绑定" + qq)) {
                BIND_DATA.remove(event.getPlayer());
                VERIFY.remove(qq).cancel();
                data.setUuid(event.getPlayer().getUniqueId().toString());
                StaticAPI.getRepository().save(data);

                event.getPlayer().sendMessage(PluginUtil.parseColor("&a绑定成功!"));
                event.setCancelled(true);
            } else if (event.getMessage().equals("确认改绑" + qq)) {
                BIND_DATA.remove(event.getPlayer());
                VERIFY.remove(qq).cancel();
                data.setUuid(event.getPlayer().getUniqueId().toString());
                StaticAPI.getRepository().update(data);

                event.getPlayer().sendMessage(PluginUtil.parseColor("&a改绑成功!"));
                event.setCancelled(true);
            }
        }

    }
}
