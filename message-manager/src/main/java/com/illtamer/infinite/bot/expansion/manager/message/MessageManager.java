package com.illtamer.infinite.bot.expansion.manager.message;

import com.illtamer.infinite.bot.expansion.manager.message.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.manager.message.listener.GroupMessageListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import lombok.Getter;

@Getter
public class MessageManager extends InfiniteExpansion {

    @Getter
    private static MessageManager instance;
    private ExpansionConfig configFile;
    private Language language;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml", this);
        this.language = Language.of("language", this);
        Placeholder.init();
        EventExecutor.registerEvents(new GroupMessageListener(), this);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "MessageManager";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

}
