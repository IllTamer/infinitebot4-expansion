package com.illtamer.infinite.bot.expansion.chatgpt;

import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.expansion.automation.AutoLoadConfiguration;
import com.illtamer.infinite.bot.minecraft.expansion.automation.annotation.ConfigClass;
import com.illtamer.infinite.bot.minecraft.expansion.automation.annotation.ConfigField;

@ConfigClass(name = "config.yml")
public class Configuration extends AutoLoadConfiguration {

    @ConfigField(ref = "setting.group-only")
    private Boolean groupOnly;

    @ConfigField(ref = "setting.prefix")
    private String prefix;

    @ConfigField(ref = "setting.timeout")
    private Integer timeout;

    @ConfigField(ref = "chatgpt.model")
    private ModelType model;

    @ConfigField(ref = "chatgpt.config.token")
    private String token;

    @ConfigField(ref = "chatgpt.config.cf-clearance")
    private String cfClearance;

    @ConfigField(ref = "chatgpt.config.user-agent")
    private String userAgent;

    @ConfigField(ref = "chatgpt.config.account.email")
    private String email;

    @ConfigField(ref = "chatgpt.config.account.password")
    private String password;

    public Configuration(IExpansion expansion) {
        super(0, expansion);
    }

    public String getCfClearance() {
        return cfClearance;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getGroupOnly() {
        return groupOnly;
    }

    public String getPrefix() {
        return prefix;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public ModelType getModel() {
        return model;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "groupOnly=" + groupOnly +
                ", prefix='" + prefix + '\'' +
                ", timeout=" + timeout +
                ", model=" + model +
                ", token='" + token + '\'' +
                ", cfClearance='" + cfClearance + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
