package com.illtamer.infinite.bot.expansion.chat.listener;

import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import org.bukkit.configuration.ConfigurationSection;

public class Group2GameListener implements Listener {

    private final boolean enable;
    private final String prefix;
    private final boolean global; // type

    public Group2GameListener(ExpansionConfig configFile) {
        ConfigurationSection section = configFile.getConfig().getConfigurationSection("group-to-game");
        if (section == null)
            section = configFile.getConfig().createSection("group-to-game");
        this.enable = section.getBoolean("enable", false);
        this.prefix = section.getString("prefix", "[]");
        this.global = "global".equalsIgnoreCase(section.getString("type"));
    }

    @EventHandler
    public void onGroup(GroupMessageEvent event) {

    }

}
