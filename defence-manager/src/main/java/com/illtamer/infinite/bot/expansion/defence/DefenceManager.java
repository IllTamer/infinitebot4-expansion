package com.illtamer.infinite.bot.expansion.defence;

import com.illtamer.infinite.bot.expansion.defence.listener.AuthListener;
import com.illtamer.infinite.bot.expansion.defence.listener.LoginListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class DefenceManager extends InfiniteExpansion {
    private static DefenceManager instance;
    private ExpansionConfig configFile;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml",  instance);
        EventExecutor.registerEvents(new AuthListener(configFile), instance);
        EventExecutor.registerBukkitEvent(new LoginListener(configFile), instance);
    }

    @Override
    public void onDisable() {
        LoginListener.unregisterTask();
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "DefenceManager";
    }

    public ExpansionConfig getConfigFile() {
        return configFile;
    }

    public static DefenceManager getInstance() {
        return instance;
    }

}
