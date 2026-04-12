package com.illtamer.infinite.bot.expansion.manager.message.listener;

import com.illtamer.infinite.bot.expansion.manager.message.config.MessageConfig;
import com.illtamer.infinite.bot.expansion.manager.message.handler.CommandMessageHandler;
import com.illtamer.infinite.bot.expansion.manager.message.handler.ImageMessageHandler;
import com.illtamer.infinite.bot.expansion.manager.message.handler.MessageHandler;
import com.illtamer.infinite.bot.expansion.manager.message.handler.MessageResponseSender;
import com.illtamer.infinite.bot.expansion.manager.message.handler.TextMessageHandler;
import com.illtamer.infinite.bot.expansion.manager.message.hook.LuckPermsHook;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public class GroupMessageListener implements Listener {

    private final List<MessageConfig> configs;
    private final Language language;
    private final MessageResponseSender responseSender;
    private final MessageHandler textHandler;
    private final MessageHandler commandHandler;
    private final MessageHandler imageHandler;

    public GroupMessageListener(List<MessageConfig> configs,
                                Language language,
                                MessageResponseSender responseSender,
                                CommandMessageHandler commandHandler,
                                ImageMessageHandler imageHandler) {
        this.configs = configs;
        this.language = language;
        this.responseSender = responseSender;
        this.textHandler = new TextMessageHandler(responseSender);
        this.commandHandler = commandHandler;
        this.imageHandler = imageHandler;
    }

    @EventHandler
    public void onMessage(GroupMessageEvent event) {
        if (!StaticAPI.inGroups(event.getGroupId())) return;

        String rawMessage = event.getRawMessage();
        if (rawMessage == null || rawMessage.isEmpty()) return;

        for (MessageConfig config : configs) {
            // 1. 条件匹配
            if (!config.getCondition().matches(rawMessage)) continue;

            // 2. 权限校验
            if (!checkPermission(event, config)) return;

            // 3. 绑定校验
            if (config.isOnlyBind() && !checkBind(event, config)) return;

            // 4. 分发处理
            switch (config.getType()) {
                case TEXT -> textHandler.handle(event, config);
                case COMMAND -> commandHandler.handle(event, config);
                case IMAGE, COMBINED -> imageHandler.handle(event, config);
            }
            return; // 匹配到第一个即停止
        }
    }

    private boolean checkPermission(GroupMessageEvent event, MessageConfig config) {
        var perm = config.getPermission();

        // 管理员校验
        if (perm.isAdmin() && !StaticAPI.isAdmin(event.getSender().getUserId())) {
            responseSender.sendText(event, config, language.get("no-permission"));
            return false;
        }

        // LuckPerms 权限校验
        if (!perm.getLuckperms().isEmpty()) {
            OfflinePlayer player = getPlayer(event);
            if (player == null) {
                responseSender.sendText(event, config, language.get("no-bind"));
                return false;
            }
            for (String permission : perm.getLuckperms()) {
                var result = LuckPermsHook.getPermission(player, permission);
                if (!result.getKey()) {
                    responseSender.sendText(event, config, language.get("no-permission"));
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkBind(GroupMessageEvent event, MessageConfig config) {
        var data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getPreferUUID() == null) {
            responseSender.sendText(event, config, language.get("no-bind"));
            return false;
        }
        return true;
    }

    private OfflinePlayer getPlayer(GroupMessageEvent event) {
        var data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getPreferUUID() == null) return null;
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(data.getPreferUUID()));
        } catch (Exception e) {
            return null;
        }
    }

}
