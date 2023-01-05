package com.illtamer.infinite.bot.expansion.landlords;

import com.illtamer.infinite.bot.expansion.landlords.config.Configuration;
import com.illtamer.infinite.bot.expansion.landlords.listener.GameListener;
import com.illtamer.infinite.bot.expansion.landlords.listener.HelpListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.automation.Registration;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class LandLordsGame extends InfiniteExpansion {

    private static LandLordsGame instance;

    @Override
    public void onEnable() {
        instance = this;
        Registration.add(new Configuration(instance), instance);
        EventExecutor.registerEvents(new GameListener(), instance);
        EventExecutor.registerEvents(new HelpListener(), instance);
    }

    @Override
    public void onDisable() {
        Registration.removeAndStoreAutoConfigs(instance);
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "LandLordsGame";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

    public static LandLordsGame getInstance() {
        return instance;
    }

}
