package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.repository.PlayerDataRepository;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.infinite.bot.minecraft.util.ValidUtil;
import com.illtamer.perpetua.sdk.Pair;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.event.request.FriendRequestEvent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class BasicBindListener implements Listener {
    private static final HashMap<Long, BukkitTask> VERIFY = new HashMap<>();
    // Pair: data->status
    private static final HashMap<Player, Pair<PlayerData, Status>> BIND_DATA = new HashMap<>();
    private final long limit;
    private final boolean autoAccessFriend;
    private final int type;
    private final Language language;

    public BasicBindListener(ExpansionConfig configFile, Language language) {
        this.limit = configFile.getConfig().getLong("bind.limit");
        this.autoAccessFriend = configFile.getConfig().getBoolean("bind.auto-access-friend");
        this.type = configFile.getConfig().getInt("bind.type");
        this.language = language;
    }

    @EventHandler
    public void onFriendRequest(FriendRequestEvent event) {
        if (autoAccessFriend) {
            event.approve(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGroupBind(GroupMessageEvent event) {
        if (!StaticAPI.inGroups(event.getGroupId())) {
            return;
        }
        String message = event.getRawMessage();
        if (message.startsWith("绑定 ")) {
            message = message.substring("绑定 ".length());
            if (type == 1) {
                event.setCancelled(true);
                checkAndBindSinglePlayer(event, message, true);
            } else if (type == 0) {
                event.setCancelled(true);
                checkAndBindSinglePlayer(event, message, false);
            } else {
                formatExceptionHandle(event);
            }
        } else if (message.startsWith("改绑 ")) {
            message = message.substring("改绑 ".length());
            if (type == 1) {
                event.setCancelled(true);
                checkAndRebindSinglePlayer(event, message, true);
            } else if (type == 0) {
                event.setCancelled(true);
                checkAndRebindSinglePlayer(event, message, false);
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
                    String message = language.get("bind", "valid-exist").replace("%player_name%", Optional.ofNullable(Lambda.nullableInvoke(OfflinePlayer::getName,
                            Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid())))).orElse("null"));
                    event.reply(message);
                    return;
                }
                status = Status.UPDATE_BIND_VALID;
            } else {
                if (data.getUuid() != null) {
                    String message = language.get("bind", "offline-exist").replace("%player_name%", Optional.ofNullable(Lambda.nullableInvoke(OfflinePlayer::getName,
                            Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid())))).orElse("null"));
                    event.reply(message);
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
            event.reply(language.get("bind", "invalid"));
            return;
        }
        Status status;
        if (valid) {
            if (data.getValidUUID() == null) {
                event.reply(language.get("bind", "invalid-valid"));
                return;
            }
            status = Status.UPDATE_CHANGE_VALID;
        } else {
            if (data.getUuid() == null) {
                event.reply(language.get("bind", "invalid-offline"));
                return;
            }
            status = Status.UPDATE_CHANGE_INVALID;
        }
        bindImplement(event::reply, player, data, limit, status, true);
    }

    private void bindImplement(Consumer<String> reply, @NotNull Player player, @NotNull PlayerData data, long limit, Status status, boolean changeBind) {
        final boolean result = ValidUtil.isValidPlayer(player);
        if ((status.valid && !result) || (!status.valid && result)) {
            reply.accept(language.get("bind", "except-status"));
            return;
        }
        BIND_DATA.put(player, new Pair<>(data, status));
        final Long userId = data.getUserId();
        VERIFY.put(userId, Bukkit.getScheduler().runTaskLater(BukkitBootstrap.getInstance(), () -> {
            final Pair<PlayerData, Status> pair = BIND_DATA.remove(player);
            if (pair == null) return;
            PlayerData remove = pair.getKey();
            VERIFY.remove(userId);
            player.sendMessage(PluginUtil.parseColor(language.get("bind", "expired").replace("%qq%", remove.getUserId().toString())));
        }, limit * 60 * 20L));
        reply.accept(language.get("bind", "process").replace("%player_name%", player.getName()));

        String keywords = "确认" + (changeBind ? "改绑" : "绑定") + userId;
        TextComponent component = new TextComponent(PluginUtil.parseColor(language.get("bind", "notice")
                .replace("%qq%", userId.toString())
                .replace("%key_word%", keywords)
                .replace("%limit%", String.valueOf(limit))
        ));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, keywords));
        player.spigot().sendMessage(component);
    }

    @Nullable
    private Player getOnlinePlayer(String playerName, MessageEvent event) {
        if (playerName.length() == 0) {
            formatExceptionHandle(event);
            return null;
        }
        final Player player = Bukkit.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            event.reply(language.get("bind", "offline").replace("%player_name%", playerName));
            return null;
        }
        return player;
    }

    private void formatExceptionHandle(MessageEvent event) {
        event.reply(language.get("bind", "mistake"));
    }

    public static class PlayerConfirmListener implements org.bukkit.event.Listener {

        private final Language language;

        public PlayerConfirmListener(Language language) {
            this.language = language;
        }

        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
        public void unbind(AsyncPlayerChatEvent event) {
            final String message = event.getMessage();
            if (!message.startsWith("解绑 ")) return;
            event.setCancelled(true);
            final Player player = event.getPlayer();
            long qq;
            try {
                qq = Long.parseLong(message.substring("解绑 ".length()));
            } catch (Exception e) {
                player.sendMessage(language.get("unbind", "user_id-mistake"));
                return;
            }
            final String uuid = player.getUniqueId().toString();
            final PlayerDataRepository repository = StaticAPI.getRepository();
            final PlayerData data = repository.queryByUUID(uuid);
            if (data == null) {
                player.sendMessage(language.get("unbind", "invalid"));
                return;
            }
            if (!data.getUserId().equals(qq)) {
                player.sendMessage(language.get("unbind", "invalid-type"));
                return;
            }
            if (data.getUuid() != null && data.getUuid().equals(uuid)) {
                data.setUuid(null);
            } else { // 正版
                data.setValidUUID(null);
            }
            repository.update(data);
            player.sendMessage(language.get("unbind", "success"));
        }

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
                    player.sendMessage(PluginUtil.parseColor(language.get("bind", "result", "bad-bind")));
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
                player.sendMessage(PluginUtil.parseColor(language.get("bind", "result", "success-bind")));
                event.setCancelled(true);
            } else if (event.getMessage().equals("确认改绑" + qq)) {
                if (status.bind) {
                    player.sendMessage(PluginUtil.parseColor(language.get("bind", "result", "bad-rebind")));
                    return;
                }
                BIND_DATA.remove(player);
                VERIFY.remove(qq).cancel();
                if (status.valid)
                    data.setValidUUID(player.getUniqueId().toString());
                else
                    data.setUuid(player.getUniqueId().toString());
                StaticAPI.getRepository().update(data);

                player.sendMessage(PluginUtil.parseColor(language.get("bind", "result", "success-rebind")));
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