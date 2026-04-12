package com.illtamer.infinite.bot.expansion.manager.message.handler;

import com.illtamer.infinite.bot.expansion.manager.message.config.MessageConfig;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;

/**
 * 消息处理器接口
 */
public interface MessageHandler {

    void handle(GroupMessageEvent event, MessageConfig config);

}
