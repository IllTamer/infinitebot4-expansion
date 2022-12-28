package com.illtamer.infinite.bot.expansion.chatgpt.listener;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.message.Message;
import com.illtamer.infinite.bot.expansion.chatgpt.Configuration;
import com.illtamer.infinite.bot.expansion.chatgpt.driver.Davinci002Handler;
import com.illtamer.infinite.bot.expansion.chatgpt.driver.Davinci003Handler;
import com.illtamer.infinite.bot.expansion.chatgpt.driver.Handler;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.automation.Registration;

import java.util.HashMap;
import java.util.Map;

public class ChatListener implements Listener {

    // userId: conversationId&timeMillion
    private final Map<Long, Pair<String, Long>> conversationMap;
    private final boolean groupOnly;
    private final String prefix;
    private final long timeoutMillionSecond;
    private Handler handler;

    public ChatListener() {
        final Configuration config = Registration.get(Configuration.class);
        this.conversationMap = new HashMap<>();
        this.groupOnly = config.getGroupOnly();
        this.prefix = config.getPrefix();
        this.timeoutMillionSecond = config.getTimeout() * 60L * 1000;
        switch (config.getModel()) {
            case TEXT_DAVINCI_002_RENDER: {
                handler = new Davinci002Handler();
                break;
            }
            case TEXT_DAVINCI_003: {
                handler = new Davinci003Handler();
                break;
            }
        }
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
            final String response = handler.getResponse(rawMessage, pair.getKey());
            conversationMap.put(userId, new Pair<>(pair.getKey(), System.currentTimeMillis()));
            event.reply(response);
        }
        event.setCancelled(true);
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
        Pair<String, String> pair = handler.createConversation(rawMessage);
        conversationMap.put(userId, new Pair<>(pair.getValue(), System.currentTimeMillis()));
        return pair.getKey();
    }

    private boolean isTimeout(long record) {
        return System.currentTimeMillis() - record >= timeoutMillionSecond;
    }

}
