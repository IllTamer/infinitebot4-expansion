package com.illtamer.infinite.bot.expansion.view;

import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.view.listener.BukkitTaskListener;
import com.illtamer.infinite.bot.expansion.view.listener.TestListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import org.bukkit.Bukkit;

public class ViewManager extends InfiniteExpansion {
    private static ViewManager instance;
    private ExpansionConfig configFile;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml",  instance);
        Assert.notNull(Bukkit.getPluginManager().getPlugin("InteractiveChatDiscordSrvAddon"), "前置插件 InteractiveChatDiscordSrvAddon 未加载");
        EventExecutor.registerEvents(new TestListener(), instance);
        EventExecutor.registerBukkitEvent(new BukkitTaskListener(), instance);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "ViewManager";
    }

    public ExpansionConfig getConfigFile() {
        return configFile;
    }

    public static ViewManager getInstance() {
        return instance;
    }

}
