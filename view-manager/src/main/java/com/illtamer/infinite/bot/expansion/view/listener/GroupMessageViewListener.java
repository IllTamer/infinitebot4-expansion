package com.illtamer.infinite.bot.expansion.view.listener;

import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.expansion.view.util.HologramUtil;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.loohp.interactivechat.objectholders.ICPlayer;
import com.loohp.interactivechat.objectholders.ICPlayerFactory;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageGeneration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

public class GroupMessageViewListener implements Listener {

    @EventHandler
    public void onOnlinePlayers(MessageEvent event) {
        final String message = event.getRawMessage();
        if (message.length() != 4 || !"当前在线".equals(message)) return;
        final BufferedImage inventoryImage = HologramUtil.getPlayerListImage();
        event.reply(MessageBuilder.json()
                .image("当前在线", HologramUtil.imageToBase64(inventoryImage))
                .build());
    }

    @EventHandler
    public void onOwnBag(MessageEvent event) throws Exception {
        final Player player = keyCheckAndGetOnlinePlayer("我的背包", event);
        if (player == null) return;
        ICPlayer icSender = ICPlayerFactory.getICPlayer(player);
        final BufferedImage inventoryImage = ImageGeneration.getPlayerInventoryImage(player.getInventory(), icSender);
        event.reply(MessageBuilder.json()
                .image(player.getName(), HologramUtil.imageToBase64(inventoryImage))
                .build());
    }

    @EventHandler
    public void onOwnEndChest(MessageEvent event) throws Exception {
        final Player player = keyCheckAndGetOnlinePlayer("我的末影箱", event);
        if (player == null) return;
        ICPlayer icSender = ICPlayerFactory.getICPlayer(player);
        final BufferedImage inventoryImage = ImageGeneration.getInventoryImage(player.getEnderChest(), icSender);
        event.reply(MessageBuilder.json()
                .image(player.getName(), HologramUtil.imageToBase64(inventoryImage))
                .build());
    }

    @Nullable
    private static Player keyCheckAndGetOnlinePlayer(@NotNull String keyword, MessageEvent event) {
        final String message = event.getRawMessage();
        if (message.length() != keyword.length() || !keyword.equals(message)) return null;
        event.setCancelled(true); // cancel event
        final PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getUuid() == null) {
            event.reply("您未绑定游戏角色");
            return null;
        }
        final Player player = data.getOfflinePlayer().getPlayer();
        if (player == null || !player.isOnline()) {
            event.reply("玩家 " + data.getOfflinePlayer().getName() + " 未在线");
            return null;
        }
        return player;
    }

}
