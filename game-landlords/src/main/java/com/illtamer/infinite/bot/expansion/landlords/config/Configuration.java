package com.illtamer.infinite.bot.expansion.landlords.config;

import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.expansion.automation.AutoLoadConfiguration;
import com.illtamer.infinite.bot.minecraft.expansion.automation.annotation.ConfigClass;
import com.illtamer.infinite.bot.minecraft.expansion.automation.annotation.ConfigField;

@ConfigClass(name = "config.yml")
public class Configuration extends AutoLoadConfiguration {

    @ConfigField(ref = "keyword.join")
    private String join;

    @ConfigField(ref = "keyword.grab")
    private String grab;

    @ConfigField(ref = "keyword.disgrab")
    private String disgrab;

    @ConfigField(ref = "keyword.send")
    private String send;

    @ConfigField(ref = "keyword.dissend")
    private String dissend;

    @ConfigField(ref = "keyword.stop")
    private String stop;

    @ConfigField(ref = "keyword.see")
    private String see;

    @ConfigField(ref = "keyword.help")
    private String help;

    public Configuration(IExpansion expansion) {
        super(expansion);
    }

    public String getHelp() {
        return help;
    }

    public String getSee() {
        return see;
    }

    public String getStop() {
        return stop;
    }

    public String getDissend() {
        return dissend;
    }

    public String getSend() {
        return send;
    }

    public String getJoin() {
        return join;
    }

    public String getGrab() {
        return grab;
    }

    public String getDisgrab() {
        return disgrab;
    }
}
