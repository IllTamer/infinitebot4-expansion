package com.illtamer.infinite.bot.expansion.github;

import com.illtamer.infinite.bot.expansion.github.task.CheckRunner;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class GithubManager extends InfiniteExpansion {

    private static GithubManager instance;
    private ExpansionConfig configFile;
    private ExpansionConfig cacheFile;
    private Language language;
    private CheckRunner checkRunner;

    @Override
    public void onEnable() {
        instance = this;
        this.language = Language.of(this);
        configFile = new ExpansionConfig("config.yml",  instance);
        cacheFile = new ExpansionConfig("cache.yml", instance);
        checkRunner = new CheckRunner(configFile);
    }

    @Override
    public void onDisable() {
        checkRunner.getFuture().cancel(true);
        instance = null;
    }

    public static GithubManager getInstance() {
        return instance;
    }

    public ExpansionConfig getConfigFile() {
        return configFile;
    }

    public ExpansionConfig getCacheFile() {
        return cacheFile;
    }

    public Language getLanguage() {
        return language;
    }

    @Override
    public String getExpansionName() {
        return "GithubManager";
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
