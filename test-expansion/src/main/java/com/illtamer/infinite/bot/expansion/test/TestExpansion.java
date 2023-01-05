package com.illtamer.infinite.bot.expansion.test;

import com.illtamer.infinite.bot.minecraft.expansion.automation.Registration;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class TestExpansion extends InfiniteExpansion {

    @Override
    public void onEnable() {
        Registration.add(new AutoConfig(this), this);
        System.out.println(Registration.get(AutoConfig.class));
    }

    @Override
    public void onDisable() {

    }

    @Override
    public String getExpansionName() {
        return "TestExpansion";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

}
