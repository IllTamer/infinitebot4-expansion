package com.illtamer.infinite.bot.expansion.chat;

import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.chat.listener.Game2GroupListener;
import com.illtamer.infinite.bot.expansion.chat.listener.Group2GameListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ChatManager extends InfiniteExpansion {
    private static ChatManager instance;
    private ExpansionConfig configFile;
    private Map<String, Object> prefixMapper;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml", instance);
        prefixMapper = Lambda.nullableInvoke(section -> section.getValues(false), configFile.getConfig().getConfigurationSection("prefix-mapper"));
        Assert.isTrue(prefixMapper != null && prefixMapper.size() != 0, "Configuration node 'prefix-mapper' can not be empty !");
        EventExecutor.registerEvents(new Group2GameListener(configFile), instance);
        EventExecutor.registerBukkitEvent(new Game2GroupListener(configFile), instance);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "ChatManager";
    }

    @NotNull
    public Map<String, Object> getPrefixMapper() {
        return prefixMapper;
    }

    public ExpansionConfig getConfigFile() {
        return configFile;
    }

    public static ChatManager getInstance() {
        return instance;
    }

}
