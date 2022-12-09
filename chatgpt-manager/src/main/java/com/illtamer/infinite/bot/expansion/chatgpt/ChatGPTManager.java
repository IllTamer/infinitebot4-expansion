package com.illtamer.infinite.bot.expansion.chatgpt;

import com.illtamer.infinite.bot.expansion.chatgpt.listener.ChatListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class ChatGPTManager extends InfiniteExpansion {

    private static ChatGPTManager instance;
    private ExpansionConfig configFile;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml", instance);
        EventExecutor.registerEvents(new ChatListener(configFile.getConfig()), instance);
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

    public ExpansionConfig getConfigFile() {
        return configFile;
    }
}
