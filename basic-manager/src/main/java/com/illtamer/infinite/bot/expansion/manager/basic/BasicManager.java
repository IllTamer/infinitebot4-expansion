package com.illtamer.infinite.bot.expansion.manager.basic;

import com.illtamer.infinite.bot.expansion.manager.basic.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.manager.basic.listener.*;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;

public class BasicManager extends InfiniteExpansion {

    private static BasicManager instance;
    private ExpansionConfig configFile;
    private Language language;

    @Override
    public void onEnable() {
        instance = this;
        Placeholder.init();
        configFile = new ExpansionConfig("config.yml", this, 2);
        this.language = Language.of("language", 1, this);
        EventExecutor.registerBukkitEvent(new TipListener(configFile), instance);
        EventExecutor.registerEvents(new SubmitListener(configFile, language), instance);
        final boolean enable = configFile.getConfig().getBoolean("member-manage.enable");
        EventExecutor.registerEvents(new BasicBindListener(configFile, language), instance);
        EventExecutor.registerBukkitEvent(new BasicBindListener.PlayerConfirmListener(language), instance);
        EventExecutor.registerEvents(new KeyWordsListener(configFile, language), instance);
        if (!enable) {
            getLogger().info("成员管理监听已取消注册");
            return;
        }
        EventExecutor.registerEvents(new MemberMenageListener(configFile), instance);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "BasicManager";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

    public ExpansionConfig getConfigFile() {
        return configFile;
    }

    public Language getLanguage() {
        return language;
    }

    public static BasicManager getInstance() {
        return instance;
    }

}
