package com.illtamer.infinite.bot.expansion.manager.message.handler;

import com.illtamer.infinite.bot.expansion.manager.message.config.MessageConfig;
import com.illtamer.infinite.bot.expansion.manager.message.hook.Placeholder;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

/**
 * 纯文本消息处理器
 */
public class TextMessageHandler implements MessageHandler {

    private final MessageResponseSender responseSender;

    public TextMessageHandler(MessageResponseSender responseSender) {
        this.responseSender = responseSender;
    }

    @Override
    public void handle(GroupMessageEvent event, MessageConfig config) {
        OfflinePlayer player = getPlayer(event);
        List<String> lines = Placeholder.set(config.getText(), player);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) sb.append("\n");
        }
        responseSender.sendText(event, config, sb.toString());
    }

    protected OfflinePlayer getPlayer(GroupMessageEvent event) {
        var data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getPreferUUID() == null) return null;
        try {
            return org.bukkit.Bukkit.getOfflinePlayer(UUID.fromString(data.getPreferUUID()));
        } catch (Exception e) {
            return null;
        }
    }

}
