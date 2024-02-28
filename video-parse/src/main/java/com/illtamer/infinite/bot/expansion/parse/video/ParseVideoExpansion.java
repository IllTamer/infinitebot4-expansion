package com.illtamer.infinite.bot.expansion.parse.video;

import com.illtamer.infinite.bot.expansion.parse.video.listener.CommandListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class ParseVideoExpansion extends InfiniteExpansion {

    private static ParseVideoExpansion instance;

    @Override
    public void onEnable() {
        instance = this;
        EventExecutor.registerEvents(new CommandListener(), instance);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "ParseVideoExpansion";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

    public static ParseVideoExpansion getInstance() {
        return instance;
    }

}
