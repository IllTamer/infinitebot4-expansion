package com.illtamer.infinite.bot.expansion.test.hook;

import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.manager.AbstractExternalExpansion;

public class IB3Expansion extends AbstractExternalExpansion {

    private final ExpansionConfig config = new ExpansionConfig("expansion.yml", this);

    @Override
    public void onEnable() {
        getLogger().info("Hello IB3Expansion");
        EventExecutor.registerEvents(new TestListener(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Good bye IB3Expansion");
    }

    @Override
    public String getExpansionName() {
        return "TestIB3Expansion";
    }

    @Override
    public String getVersion() {
        return "1.0-SNAPSHOT";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

}
