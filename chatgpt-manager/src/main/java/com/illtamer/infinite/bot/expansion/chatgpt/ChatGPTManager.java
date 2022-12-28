package com.illtamer.infinite.bot.expansion.chatgpt;

import com.illtamer.infinite.bot.expansion.chatgpt.listener.ChatListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.automation.Registration;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class ChatGPTManager extends InfiniteExpansion {

    private static ChatGPTManager instance;

    @Override
    public void onEnable() {
        instance = this;
        Registration.add(new Configuration(instance), instance);
        EventExecutor.registerEvents(new ChatListener(), instance);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "ChatGPTManager";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

}
