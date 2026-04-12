package com.illtamer.infinite.bot.expansion.manager.message.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.regex.Pattern;

@Getter
public class ConditionConfig {

    private final Pattern regex;
    private final List<String> keywords;

    public ConditionConfig(ConfigurationSection section) {
        String regexStr = section.getString("regex", "");
        this.regex = (regexStr != null && !regexStr.isEmpty()) ? Pattern.compile(regexStr) : null;
        this.keywords = section.getStringList("keyword");
    }

    public boolean matches(String message) {
        if (regex != null && regex.matcher(message).matches()) {
            return true;
        }
        for (String keyword : keywords) {
            if (keyword.equals(message)) {
                return true;
            }
        }
        return false;
    }

}
