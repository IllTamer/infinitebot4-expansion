package com.illtamer.infinite.bot.expansion.manager.message.handler;

import com.illtamer.infinite.bot.expansion.manager.message.config.MessageConfig;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionLogger;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;
import com.illtamer.perpetua.sdk.message.Message;

/**
 * 统一消息响应发送器：根据配置决定 reply 或普通群消息发送
 */
public class MessageResponseSender {

    private final ExpansionLogger logger;

    public MessageResponseSender(ExpansionLogger logger) {
        this.logger = logger;
    }

    public void sendText(GroupMessageEvent event, MessageConfig config, String text) {
        if (text == null || text.isEmpty()) return;
        try {
            if (config.isReply()) {
                event.reply(text);
            } else {
                OpenAPIHandling.sendGroupMessage(text, event.getGroupId());
            }
        } catch (Exception e) {
            logger.warn("[MessageManager] 发送文本消息失败: " + e.getMessage());
        }
    }

    public void sendMessage(GroupMessageEvent event, MessageConfig config, Message message) {
        if (message == null) return;
        try {
            if (config.isReply()) {
                event.reply(message);
            } else {
                OpenAPIHandling.sendGroupMessage(message, event.getGroupId());
            }
        } catch (Exception e) {
            logger.warn("[MessageManager] 发送富文本消息失败: " + e.getMessage());
        }
    }

}
