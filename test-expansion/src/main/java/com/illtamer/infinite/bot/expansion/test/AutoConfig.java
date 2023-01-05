package com.illtamer.infinite.bot.expansion.test;

import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.expansion.automation.AutoLoadConfiguration;
import com.illtamer.infinite.bot.minecraft.expansion.automation.annotation.ConfigClass;
import com.illtamer.infinite.bot.minecraft.expansion.automation.annotation.ConfigField;

import java.util.Map;

@ConfigClass(name = "config.yml")
public class AutoConfig extends AutoLoadConfiguration {

    private Boolean check;

    @ConfigField(ref = "config.point.name")
    private String name;

    @ConfigField(ref = "config.point")
    private Map<String, Object> map;

    public AutoConfig(IExpansion expansion) {
        super(0, expansion);
    }

    public Boolean isCheck() {
        return check;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return "AutoConfig{" +
                "check=" + check +
                ", name='" + name + '\'' +
                ", map=" + map +
                '}';
    }
}
