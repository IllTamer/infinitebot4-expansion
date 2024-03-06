package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.illtamer.infinite.bot.expansion.manager.basic.hook.Placeholder;
import com.illtamer.infinite.bot.minecraft.configuration.config.BotConfiguration;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TipListener implements Listener {

    private final String joinMessage;
    private final String quitMessage;

    public TipListener(ExpansionConfig configFile) {
        final ConfigurationSection tip = configFile.getConfig().getConfigurationSection("tip");
        this.joinMessage = tip.getBoolean("join.enable") ? tip.getString("join.message") : null;
        this.quitMessage = tip.getBoolean("quit.enable") ? tip.getString("quit.message") : null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (joinMessage == null) return;
        BotConfiguration.main.groups.forEach(group ->
                OpenAPIHandling.sendGroupMessage(Placeholder.set(joinMessage, event.getPlayer()), group));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (quitMessage == null) return;
        BotConfiguration.main.groups.forEach(group ->
                OpenAPIHandling.sendGroupMessage(Placeholder.set(quitMessage, event.getPlayer()), group));
    }

}