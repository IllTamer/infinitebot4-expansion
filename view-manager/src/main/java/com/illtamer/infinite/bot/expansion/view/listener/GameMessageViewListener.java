package com.illtamer.infinite.bot.expansion.view.listener;

import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.loohp.interactivechatdiscordsrvaddon.api.InteractiveChatDiscordSrvAddonAPI;
import com.loohp.interactivechatdiscordsrvaddon.wrappers.GraphicsToPacketMapWrapper;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class GameMessageViewListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onMessage(AsyncPlayerChatEvent event) {
        final String message = event.getMessage();

    }

    @EventHandler
    public void onShowMapImage(AsyncPlayerChatEvent event) {
        final String message = event.getMessage();
        if (!message.startsWith("vm-map#")) return;
        final GraphicsToPacketMapWrapper wrapper = InteractiveChatDiscordSrvAddonAPI.
                getDiscordImageWrapperByUUID(UUID.fromString(message.substring("vm-map#".length())));
        Bukkit.getScheduler().runTask(Bootstrap.getInstance(), () -> {
            wrapper.show(event.getPlayer());
        });
        event.setCancelled(true);
    }

}
