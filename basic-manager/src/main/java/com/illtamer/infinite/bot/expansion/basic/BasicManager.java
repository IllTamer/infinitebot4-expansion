package com.illtamer.infinite.bot.expansion.basic;

import com.illtamer.infinite.bot.expansion.basic.listener.BasicBindListener;
import com.illtamer.infinite.bot.expansion.basic.listener.KeyWordsListener;
import com.illtamer.infinite.bot.expansion.basic.listener.MemberMenageListener;
import com.illtamer.infinite.bot.expansion.basic.listener.SubmitListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class BasicManager extends InfiniteExpansion {

    private static BasicManager instance;
    private ExpansionConfig configFile;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml", this);
        EventExecutor.registerEvents(new SubmitListener(configFile), instance);
        EventExecutor.registerEvents(new MemberMenageListener(configFile), instance);
        EventExecutor.registerEvents(new BasicBindListener(configFile), instance);
        EventExecutor.registerBukkitEvent(new BasicBindListener.PlayerConfirmListener(), instance);
        EventExecutor.registerEvents(new KeyWordsListener(), instance);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "BasicManager";
    }

    public ExpansionConfig getConfigFile() {
        return configFile;
    }

    public static BasicManager getInstance() {
        return instance;
    }

}
