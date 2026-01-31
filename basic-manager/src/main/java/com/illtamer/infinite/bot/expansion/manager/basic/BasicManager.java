package com.illtamer.infinite.bot.expansion.manager.basic;

import com.illtamer.infinite.bot.expansion.manager.basic.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.manager.basic.listener.*;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import com.illtamer.infinite.bot.minecraft.pojo.TimedBlockingCache;
import lombok.Getter;

@Getter
public class BasicManager extends InfiniteExpansion {

    @Getter
    private static BasicManager instance;
    private ExpansionConfig configFile;
    private Language language;
    private DistributeHelper distributeHelper;


    @Override
    public void onEnable() {
        instance = this;
        distributeHelper = new DistributeHelper(this);
        Placeholder.init();
        configFile = new ExpansionConfig("config.yml", this, 3);
        this.language = Language.of("language", 2, this);
        EventExecutor.registerBukkitEvent(new TipListener(configFile), instance);
        EventExecutor.registerEvents(new SubmitListener(configFile, language), instance);
        final boolean enable = configFile.getConfig().getBoolean("member-manage.enable");
        EventExecutor.registerEvents(new BasicBindListener(configFile, language), instance);
        EventExecutor.registerBukkitEvent(new BasicBindListener.PlayerConfirmListener(language), instance);
        EventExecutor.registerEvents(distributeHelper.newBroadCastListener(
                KeyWordsListener::getPlayerListJson, KeyWordsListener.ON_SHOW_PLAYERS), instance);
        EventExecutor.registerEvents(distributeHelper.newBroadCastListener(
                /*TODO*/, KeyWordsListener.ON_LOGIN_OUT), instance);
        EventExecutor.registerEvents(new KeyWordsListener(configFile, language, distributeHelper), instance);
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
        return "2.1";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

}
