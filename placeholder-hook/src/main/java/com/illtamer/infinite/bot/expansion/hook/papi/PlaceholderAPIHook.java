package com.illtamer.infinite.bot.expansion.hook.papi;

import com.illtamer.infinite.bot.expansion.hook.papi.hook.PAPIHook;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class PlaceholderAPIHook extends InfiniteExpansion {

    private static PlaceholderAPIHook instance;

    @Override
    public void onEnable() {
        instance = this;
        if (!PAPIHook.tryRegister()) {
            getLogger().error("Placeholder API 变量注册失败，附属功能已禁用");
        }
    }

    @Override
    public void onDisable() {
        PAPIHook.tryUnregister();
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "PlaceholderAPIHook";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

    public static PlaceholderAPIHook getInstance() {
        return instance;
    }

}
