package com.illtamer.infinite.bot.expansion.message;

import com.illtamer.infinite.bot.expansion.message.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.message.listener.MessageListener;
import com.illtamer.infinite.bot.expansion.message.message.MessageLimit;
import com.illtamer.infinite.bot.expansion.message.message.MessageLoader;
import com.illtamer.infinite.bot.expansion.message.message.ResponseHandler;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageManager extends InfiniteExpansion {

    private static MessageManager instance;
    private ExpansionConfig configFile;
    private Language language;

    @Override
    public void onEnable() {
        instance = this;
        this.language = Language.of(this);
        configFile = new ExpansionConfig("config.yml",  instance);
        if (!Placeholder.init())
            getLogger().warn("未检测到 PlaceholderAPI 前置，相关变量将不可用！");
        final FileConfiguration config = configFile.getConfig();
        MessageLimit.init(config, language);
        ResponseHandler.init(language);
        MessageLoader.init(config, instance.getDataFolder(), instance);
        EventExecutor.registerEvents(new MessageListener(), instance);
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
