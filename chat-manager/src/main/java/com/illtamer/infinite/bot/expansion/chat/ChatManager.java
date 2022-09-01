package com.illtamer.infinite.bot.expansion.chat;

import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.chat.filter.AccessStartFilter;
import com.illtamer.infinite.bot.expansion.chat.filter.DenyContainsFilter;
import com.illtamer.infinite.bot.expansion.chat.filter.MessageFilter;
import com.illtamer.infinite.bot.expansion.chat.listener.Game2GroupListener;
import com.illtamer.infinite.bot.expansion.chat.listener.GameMessageViewListener;
import com.illtamer.infinite.bot.expansion.chat.listener.Group2GameListener;
import com.illtamer.infinite.bot.expansion.chat.util.AtUtil;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ChatManager extends InfiniteExpansion {
    private static ChatManager instance;
    private ExpansionConfig configFile;
    private ExpansionConfig closeFile;
    private Language language;
    private Map<String, Object> prefixMapper;

    @Override
    public void onEnable() {
        instance = this;
        Global.init(instance);
        configFile = new ExpansionConfig("config.yml", instance);
        closeFile = new ExpansionConfig("close.yml", instance);
        AtUtil.init(configFile);
        prefixMapper = Lambda.nullableInvoke(section -> section.getValues(false), configFile.getConfig().getConfigurationSection("prefix-mapper"));
        Assert.isTrue(prefixMapper != null && prefixMapper.size() != 0, "Configuration node 'prefix-mapper' can not be empty !");
        MessageFilter.MAP.put("deny-contains", new DenyContainsFilter());
        MessageFilter.MAP.put("access-start", new AccessStartFilter());
        this.language = Language.of(this);
        EventExecutor.registerEvents(new Group2GameListener(configFile, language), instance);
        EventExecutor.registerBukkitEvent(new Game2GroupListener(configFile, language), instance);
        boolean expandChat = configFile.getConfig().getBoolean("expand-chat") && StaticAPI.hasExpansion("ViewManager", "IllTamer");
        if (expandChat) {
            EventExecutor.registerBukkitEvent(new GameMessageViewListener(), instance);
        }
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

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
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

    public Language getLanguage() {
        return language;
    }

    public static ChatManager getInstance() {
        return instance;
    }

}
