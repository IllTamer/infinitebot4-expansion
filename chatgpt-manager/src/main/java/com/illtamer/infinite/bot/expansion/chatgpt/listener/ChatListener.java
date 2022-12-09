package com.illtamer.infinite.bot.expansion.chatgpt.listener;

import com.github.plexpt.chatgpt.Chatbot;
import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.message.Message;
import com.illtamer.infinite.bot.api.util.HttpRequestUtil;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ChatListener implements Listener {

    // userId: conversationId&timeMillion
    private final Map<Long, Pair<String, Long>> conversationMap;
    private final Chatbot chatbot;
    private final boolean groupOnly;
    private final String prefix;
    private final long timeoutMillionSecond;

    public ChatListener(FileConfiguration config) {
        final Pair<Integer, String> pair = HttpRequestUtil.getJson(config.getString("token-source"), null);
        if (pair.getKey() != 200) {
            throw new IllegalArgumentException("token 资源不可用，state: " + pair.getKey());
        }
        this.conversationMap = new HashMap<>();
        this.chatbot = new Chatbot(pair.getValue());
        this.groupOnly = config.getBoolean("group-only");
        this.prefix = config.getString("prefix");
        this.timeoutMillionSecond = config.getInt("timeout") * 60L * 1000;
    }

    @EventHandler
    public void onChat(MessageEvent event) {
        if (event.isCancelled()) return;
        if (groupOnly) {
            if (!(event instanceof GroupMessageEvent)) return;
            if (!StaticAPI.inGroups(((GroupMessageEvent) event).getGroupId())) return;
        }
        final String rawMessage = event.getRawMessage();
        final Long userId = event.getUserId();
        final Pair<String, Long> pair = conversationMap.get(userId);
        if (pair == null) {
            if (!rawMessage.startsWith(prefix)) return;
            event.reply(createConversation(rawMessage, userId));
        } else {
            // timeout
            if (isTimeout(pair.getValue())) {
                conversationMap.remove(userId);
                return;
            }
            chatbot.setConversationId(pair.getKey());
            final Map<String, Object> response = chatbot.getChatResponse(rawMessage);
            conversationMap.put(userId, new Pair<>(pair.getKey(), System.currentTimeMillis()));
            event.reply((String) response.get("message"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClose(MessageEvent event) {
        final Message message = event.getMessage();
        if (message.isTextOnly() && "关闭提问".equals(event.getRawMessage())) {
            Pair<String, Long> pair;
            if ((pair = conversationMap.remove(event.getUserId())) != null && !isTimeout(pair.getValue())) {
                event.reply("对话已关闭");
            } else {
                event.reply("您没有正在进行的对话");
            }
            event.setCancelled(true);
        }
    }

    private String createConversation(String rawMessage, long userId) {
        if (rawMessage.startsWith(prefix)) {
            rawMessage = rawMessage.substring(prefix.length());
        }
        final Map<String, Object> response = chatbot.getChatResponse(rawMessage);
        conversationMap.put(userId, new Pair<>((String) response.get("conversation_id"), System.currentTimeMillis()));
        return (String) response.get("message");
    }

    private boolean isTimeout(long record) {
        return System.currentTimeMillis() - record >= timeoutMillionSecond;
    }

}
