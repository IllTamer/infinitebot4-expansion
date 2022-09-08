package com.illtamer.infinite.bot.expansion.message;

import com.illtamer.infinite.bot.expansion.message.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.message.listener.MessageListener;
import com.illtamer.infinite.bot.expansion.message.message.MessageLoader;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class MessageManager extends InfiniteExpansion {

    private static MessageManager instance;
    private ExpansionConfig configFile;
    private Language language;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml",  instance);
        if (!Placeholder.init())
            getLogger().warn("未检测到 PlaceholderAPI 前置，相关变量将不可用！");
        MessageLoader.init(configFile.getConfig(), instance.getDataFolder(), instance);
        EventExecutor.registerEvents(new MessageListener(), instance);
        this.language = Language.of(this);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static MessageManager getInstance() {
        return instance;
    }

    public ExpansionConfig getConfigFile() {
        return configFile;
    }

    public Language getLanguage() {
        return language;
    }

    @Override
    public String getExpansionName() {
        return "MessageManager";
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
