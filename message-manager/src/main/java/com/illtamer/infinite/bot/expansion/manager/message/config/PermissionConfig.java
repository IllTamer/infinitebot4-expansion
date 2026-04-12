package com.illtamer.infinite.bot.expansion.manager.message.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
public class PermissionConfig {

    private final boolean admin;
    private final List<String> luckperms;

    public PermissionConfig(ConfigurationSection section) {
        this.admin = section.getBoolean("admin", false);
        this.luckperms = section.getStringList("luckperms");
    }

}
