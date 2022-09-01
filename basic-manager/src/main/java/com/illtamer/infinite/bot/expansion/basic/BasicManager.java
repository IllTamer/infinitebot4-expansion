package com.illtamer.infinite.bot.expansion.basic;

import com.illtamer.infinite.bot.expansion.basic.listener.BasicBindListener;
import com.illtamer.infinite.bot.expansion.basic.listener.KeyWordsListener;
import com.illtamer.infinite.bot.expansion.basic.listener.MemberMenageListener;
import com.illtamer.infinite.bot.expansion.basic.listener.SubmitListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class BasicManager extends InfiniteExpansion {

    private static BasicManager instance;
    private ExpansionConfig configFile;
    private Language language;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml", this);
        this.language = Language.of(this);
        EventExecutor.registerEvents(new SubmitListener(configFile, language), instance);
        EventExecutor.registerEvents(new MemberMenageListener(configFile), instance);
        EventExecutor.registerEvents(new BasicBindListener(configFile, language), instance);
        EventExecutor.registerBukkitEvent(new BasicBindListener.PlayerConfirmListener(language), instance);
        EventExecutor.registerEvents(new KeyWordsListener(configFile, language), instance);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "BasicManager";
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

    public Language getLanguage() {
        return language;
    }

    public static BasicManager getInstance() {
        return instance;
    }

}
