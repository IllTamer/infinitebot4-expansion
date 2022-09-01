package com.illtamer.infinite.bot.expansion.view;

import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.view.listener.GroupMessageViewListener;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewManager extends InfiniteExpansion {
    private static ViewManager instance;
    private ExpansionConfig configFile;
    private Language language;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml",  instance);
        this.language = Language.of(this);
        Assert.notNull(Bukkit.getPluginManager().getPlugin("InteractiveChatDiscordSrvAddon"), "前置插件 InteractiveChatDiscordSrvAddon 未加载");
        if (!StaticAPI.hasExpansion("ChatManager", "IllTamer"))
            getLogger().warn("未检测到消息互通附属，功能增强已关闭");
        else
            getLogger().info("检测到消息互通附属，功能增强已开启");
        EventExecutor.registerEvents(new GroupMessageViewListener(configFile, language), instance);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "ViewManager";
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

    public static ViewManager getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        final Matcher matcher = Pattern.compile("(?:<(cmd|chat)=([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(:(.*?):)?>)").matcher("<chat=57876712-e6a6-4cb2-ad64-19137f209beb:[i]:>");
        if (matcher.matches())
            for (int i = 0; i < matcher.groupCount(); i++) {
                System.out.println(matcher.group(i));
            }
    }

}
