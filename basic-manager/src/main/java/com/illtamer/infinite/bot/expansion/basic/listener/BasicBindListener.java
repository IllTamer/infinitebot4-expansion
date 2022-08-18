package com.illtamer.infinite.bot.expansion.basic.listener;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.api.event.Priority;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.infinite.bot.minecraft.util.ValidUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class BasicBindListener implements Listener {
    private static final HashMap<Long, BukkitTask> VERIFY = new HashMap<>();
    // Pair: data->status
    private static final HashMap<Player, Pair<PlayerData, Status>> BIND_DATA = new HashMap<>();
    private final long limit;

    public BasicBindListener(ExpansionConfig configFile) {
        this.limit = configFile.getConfig().getLong("bind.limit");
    }

    @EventHandler(priority = Priority.HIGHEST)
    public void onGroupBind(GroupMessageEvent event) {
        if (!StaticAPI.inGroups(event.getGroupId())) {
            return;
        }
        String message = event.getRawMessage();
        if (message.startsWith("绑定 ")) {
            message = message.substring("绑定 ".length());
            if (message.startsWith("正版 ")) {
                event.setCancelled(true);
                checkAndBindSinglePlayer(event, message.substring("正版 ".length()), true);
            } else if (message.startsWith("离线 ")) {
                event.setCancelled(true);
                checkAndBindSinglePlayer(event, message.substring("离线 ".length()), false);
            } else {
                formatExceptionHandle(event);
            }
        } else if (message.startsWith("改绑 ")) {
            message = message.substring("改绑 ".length());
            if (message.startsWith("正版 ")) {
                event.setCancelled(true);
                checkAndRebindSinglePlayer(event, message.substring("正版 ".length()), true);
            } else if (message.startsWith("离线 ")) {
                event.setCancelled(true);
                checkAndRebindSinglePlayer(event, message.substring("离线 ".length()), false);
            } else {
                formatExceptionHandle(event);
            }
        }
    }

    private void checkAndBindSinglePlayer(MessageEvent event, String playerName, boolean valid) {
        Player player;
        if ((player = getOnlinePlayer(playerName, event)) == null) return;
        final long userId = event.getSender().getUserId();
        PlayerData data = StaticAPI.getRepository().queryByUserId(userId);
        Status status;
        if (data == null) {
            status = valid ? Status.INSERT_BIND_VALID : Status.INSERT_BIND_INVALID;
            data = new PlayerData();
            data.setUserId(userId);
        } else {
            if (valid) {
                if (data.getValidUUID() != null) {
                    event.reply("您已绑定正版玩家: " + Lambda.nullableInvoke(OfflinePlayer::getName, Bukkit.getOfflinePlayer(UUID.fromString(data.getValidUUID()))));
                    return;
                }
                status = Status.UPDATE_BIND_VALID;
            } else {
                if (data.getUuid() != null) {
                    event.reply("您已绑定离线玩家: " + Lambda.nullableInvoke(OfflinePlayer::getName, Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid()))));
                    return;
                }
                status = Status.UPDATE_BIND_INVALID;
            }
        }
        bindImplement(event::reply, player, data, limit, status, false);
    }

    private void checkAndRebindSinglePlayer(MessageEvent event, String playerName, boolean valid) {
        Player player;
        if ((player = getOnlinePlayer(playerName, event)) == null) return;
        PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null) {
            event.reply("您未绑定过玩家");
            return;
        }
        Status status;
        if (valid) {
            if (data.getValidUUID() == null) {
                event.reply("您未绑定过正版玩家");
                return;
            }
            status = Status.UPDATE_CHANGE_VALID;
        } else {
            if (data.getUuid() == null) {
                event.reply("您未绑定过离线玩家");
                return;
            }
            status = Status.UPDATE_CHANGE_INVALID;
        }
        bindImplement(event::reply, player, data, limit, status, true);
    }

    private static void bindImplement(Consumer<String> reply, @NotNull Player player, @NotNull PlayerData data, long limit, Status status, boolean changeBind) {
        final boolean result = ValidUtil.isValidPlayer(player);
        if ((status.valid && !result) || (!status.valid && result)) {
            reply.accept("账号类型与绑定类型不符!");
            return;
        }
        BIND_DATA.put(player, new Pair<>(data, status));
        final Long userId = data.getUserId();
        VERIFY.put(userId, Bukkit.getScheduler().runTaskLater(Bootstrap.getInstance(), () -> {
            final Pair<PlayerData, Status> pair = BIND_DATA.remove(player);
            if (pair == null) return;
            PlayerData remove = pair.getKey();
            VERIFY.remove(userId);
            player.sendMessage(PluginUtil.parseColor("&cQQ(&f" + remove.getUserId() + "&c)向此账号申请的绑定已过期"));
        }, limit * 60 * 20L));
        reply.accept("已向 " + player.getName() + " 发送申请, 请前往游戏进行验证!");
        player.sendMessage(PluginUtil.parseColor("&eQQ(&f" + userId + "&e) 正向您申请绑定, 请核实后输入 &a确认" + (changeBind ? "改绑" : "绑定") + userId + " &e完成与该账号的绑定! &7&l[" + limit + "分钟内有效]"));
    }

    @Nullable
    private static Player getOnlinePlayer(String playerName, MessageEvent event) {
        if (playerName.length() == 0) {
            formatExceptionHandle(event);
            return null;
        }
        final Player player = Bukkit.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            event.reply("玩家: " + playerName + " 未在线");
            return null;
        }
        return player;
    }

    private static void formatExceptionHandle(MessageEvent event) {
        event.reply("请使用 '绑定/改绑 正版/离线 玩家名称' 的格式进行更改绑定!");
    }

    public static class PlayerConfirmListener implements org.bukkit.event.Listener {

        @org.bukkit.event.EventHandler
        public void onConfirm(AsyncPlayerChatEvent event) {
            final Player player = event.getPlayer();
            Pair<PlayerData, Status> pair = BIND_DATA.get(player);
            if (pair == null) return;
            final PlayerData data = pair.getKey();
            final Status status = pair.getValue();
            long qq = data.getUserId();
            if (event.getMessage().equals("确认绑定" + qq)) {
                if (!status.bind) {
                    player.sendMessage("关键字回复错误，您当前状态为：待绑定");
                    return;
                }
                BIND_DATA.remove(player);
                VERIFY.remove(qq).cancel();
                if (status.valid)
                    data.setValidUUID(player.getUniqueId().toString());
                else
                    data.setUuid(player.getUniqueId().toString());
                if (status.insert)
                    StaticAPI.getRepository().save(data);
                else
                    StaticAPI.getRepository().update(data);
                player.sendMessage(PluginUtil.parseColor("&a绑定成功!"));
                event.setCancelled(true);
            } else if (event.getMessage().equals("确认改绑" + qq)) {
                if (status.bind) {
                    player.sendMessage("关键字回复错误，您当前状态为：待改绑");
                    return;
                }
                BIND_DATA.remove(player);
                VERIFY.remove(qq).cancel();
                if (status.valid)
                    data.setValidUUID(player.getUniqueId().toString());
                else
                    data.setUuid(player.getUniqueId().toString());
                StaticAPI.getRepository().update(data);

                player.sendMessage(PluginUtil.parseColor("&a改绑成功!"));
                event.setCancelled(true);
            }
        }

    }

    private enum Status {

        INSERT_BIND_VALID(true, true, true),

        INSERT_BIND_INVALID(false, true, true),

        UPDATE_CHANGE_VALID(true, false, false),

        UPDATE_CHANGE_INVALID(false, false, false),

        UPDATE_BIND_VALID(true, true, false),

        UPDATE_BIND_INVALID(false, true, false);

        private final boolean valid;
        private final boolean bind;
        private final boolean insert;

        Status(boolean valid, boolean bind, boolean insert) {
            this.valid = valid;
            this.bind = bind;
            this.insert = insert;
        }

    }

}
