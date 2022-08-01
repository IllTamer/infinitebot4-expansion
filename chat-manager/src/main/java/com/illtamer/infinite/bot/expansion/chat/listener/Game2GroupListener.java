package com.illtamer.infinite.bot.expansion.chat.listener;

import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class Game2GroupListener implements Listener {

    private final boolean enable;
    private final String prefix;

    public Game2GroupListener(ExpansionConfig configFile) {
        ConfigurationSection section = configFile.getConfig().getConfigurationSection("game-to-group");
        if (section == null)
            section = configFile.getConfig().createSection("game-to-group");
        this.enable = section.getBoolean("enable", false);
        this.prefix = section.getString("prefix", "[]");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

    }

}
