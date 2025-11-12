package com.illtamer.infinite.bot.expansion.manager.ip;

import com.illtamer.infinite.bot.expansion.manager.ip.listener.GameListener;
import com.illtamer.infinite.bot.expansion.manager.ip.listener.GroupListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class IPManager extends InfiniteExpansion {

    @Getter
    private static IPManager instance;
    private ExpansionConfig configFile;
    private Language language;

    // qq -> obj
    private final Map<Long, BindData> bind = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml", instance);
        language = Language.of(this);
        EventExecutor.registerEvents(new GroupListener(instance), instance);
        EventExecutor.registerBukkitEvent(new GameListener(instance), instance);
    }

    @Override
    public void onDisable() {
        configFile.save();
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "IPManager";
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
