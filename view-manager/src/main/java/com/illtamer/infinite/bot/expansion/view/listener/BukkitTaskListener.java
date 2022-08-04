package com.illtamer.infinite.bot.expansion.view.listener;

import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.loohp.interactivechatdiscordsrvaddon.api.InteractiveChatDiscordSrvAddonAPI;
import com.loohp.interactivechatdiscordsrvaddon.wrappers.GraphicsToPacketMapWrapper;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class BukkitTaskListener implements Listener {

    @EventHandler
    public void test(AsyncPlayerChatEvent event) {
        final String message = event.getMessage();
        if (!message.startsWith("test ")) return;
        final GraphicsToPacketMapWrapper wrapper = InteractiveChatDiscordSrvAddonAPI.getDiscordImageWrapperByUUID(UUID.fromString(message.substring("test ".length())));
        Bukkit.getScheduler().runTask(Bootstrap.getInstance(), () -> {
            wrapper.show(event.getPlayer());
        });
    }

}
