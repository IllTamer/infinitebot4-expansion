package com.illtamer.infinite.bot.expansion.chat.listener;

import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.expansion.chat.event.PreGame2GroupMessageEvent;
import com.illtamer.infinite.bot.expansion.view.util.HologramUtil;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.objectholders.ICPlayer;
import com.loohp.interactivechat.objectholders.ICPlayerFactory;
import com.loohp.interactivechatdiscordsrvaddon.api.InteractiveChatDiscordSrvAddonAPI;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageGeneration;
import com.loohp.interactivechatdiscordsrvaddon.wrappers.GraphicsToPacketMapWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GameMessageViewListener implements Listener {

    @EventHandler
    public void onMessage(PreGame2GroupMessageEvent event) {
        if (event.getCleanMessage().length() == 0) return;
        final Player player = event.getPlayer();
        final ICPlayer icPlayer = ICPlayerFactory.getICPlayer(player);
        String message = event.getCleanMessage();
        boolean item = false, inv = false, end = false;

        if (InteractiveChat.itemPlaceholder.matcher(message).find()) {
            message = message.replaceAll(InteractiveChat.itemPlaceholder.pattern(), "");
            item = true;
        }
        if (InteractiveChat.invPlaceholder.matcher(message).find()) {
            message = message.replaceAll(InteractiveChat.invPlaceholder.pattern(), "");
            inv = true;
        }
        if (InteractiveChat.enderPlaceholder.matcher(message).find()) {
            message = message.replaceAll(InteractiveChat.enderPlaceholder.pattern(), "");
            end = true;
        }
        final MessageBuilder builder = MessageBuilder.json()
                .text(event.getPrefix())
                .text(message);
        try {
            if (item) {
                final ItemStack mainHand = player.getItemInHand();
                builder.image("item", HologramUtil.imageToBase64(ImageGeneration.getItemStackImage(mainHand, icPlayer)))
                        .image("tooltip", HologramUtil.imageToBase64(HologramUtil.getTooltip(mainHand, player)));
            }
            if (inv) builder.image("inv", HologramUtil.imageToBase64(ImageGeneration.getPlayerInventoryImage(player.getInventory(), icPlayer)));
            if (end) builder.image("end", HologramUtil.imageToBase64(ImageGeneration.getInventoryImage(player.getEnderChest(), icPlayer)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        event.setMessage(builder.build());
//        if (item || inv || end)
//            event.cancelledOriEvent();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onShowMapImage(AsyncPlayerChatEvent event) {
        final String message = event.getMessage();
        if (!message.startsWith("vm-map//")) return;
        final GraphicsToPacketMapWrapper wrapper = InteractiveChatDiscordSrvAddonAPI.
                getDiscordImageWrapperByUUID(UUID.fromString(message.substring("vm-map//".length())));
        Bukkit.getScheduler().runTask(Bootstrap.getInstance(), () ->
                wrapper.show(event.getPlayer()));
        event.setCancelled(true);
    }

}
