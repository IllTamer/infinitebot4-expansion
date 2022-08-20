package com.illtamer.infinite.bot.expansion.chat;

import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.chat.filter.AccessStartFilter;
import com.illtamer.infinite.bot.expansion.chat.filter.DenyContainsFilter;
import com.illtamer.infinite.bot.expansion.chat.filter.Filter;
import com.illtamer.infinite.bot.expansion.chat.listener.Game2GroupListener;
import com.illtamer.infinite.bot.expansion.chat.listener.GameMessageViewListener;
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
    private ExpansionConfig closeFile;
    private Map<String, Object> prefixMapper;

    @Override
    public void onEnable() {
        instance = this;
        Global.init(this);
        configFile = new ExpansionConfig("config.yml", instance);
        closeFile = new ExpansionConfig("close.yml", instance);
        prefixMapper = Lambda.nullableInvoke(section -> section.getValues(false), configFile.getConfig().getConfigurationSection("prefix-mapper"));
        Assert.isTrue(prefixMapper != null && prefixMapper.size() != 0, "Configuration node 'prefix-mapper' can not be empty !");
        Filter.MAP.put("deny-contains", new DenyContainsFilter());
        Filter.MAP.put("access-start", new AccessStartFilter());
        EventExecutor.registerEvents(new Group2GameListener(configFile), instance);
        EventExecutor.registerBukkitEvent(new Game2GroupListener(configFile), instance);
        EventExecutor.registerBukkitEvent(new GameMessageViewListener(configFile), instance);
    }

    @Override
    public void onDisable() {
        Global.save(closeFile);
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

    public ExpansionConfig getCloseFile() {
        return closeFile;
    }

    public ExpansionConfig getConfigFile() {
        return configFile;
    }

    public static ChatManager getInstance() {
        return instance;
    }

}
