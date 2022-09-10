package com.illtamer.infinite.bot.expansion.view.listener;

import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.expansion.view.util.HologramUtil;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.loohp.interactivechat.objectholders.ICPlayer;
import com.loohp.interactivechat.objectholders.ICPlayerFactory;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageGeneration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.UUID;

public class GroupMessageViewListener implements Listener {

    private final String myBag;
    private final String myEnder;
    private final String online;
    private final Language language;

    public GroupMessageViewListener(ExpansionConfig config, Language language) {
        final ConfigurationSection section = config.getConfig().getConfigurationSection("key-word");
        this.myBag = section.getString("my-bag");
        this.myEnder = section.getString("my-ender");
        this.online = section.getString("online");
        this.language = language;
    }

    @EventHandler
    public void onOnlinePlayers(MessageEvent event) {
        final String message = event.getRawMessage();
        if (message.length() != 4 || !online.equals(message)) return;
        final BufferedImage inventoryImage = HologramUtil.getPlayerListImage();
        event.reply(MessageBuilder.json()
                .image(online, HologramUtil.imageToBase64(inventoryImage))
                .build());
    }

    @EventHandler
    public void onOwnBag(MessageEvent event) throws Exception {
        final Player player = keyCheckAndGetOnlinePlayer(myBag, event);
        if (player == null) return;
        ICPlayer icSender = ICPlayerFactory.getICPlayer(player);
        final BufferedImage inventoryImage = ImageGeneration.getPlayerInventoryImage(player.getInventory(), icSender);
        event.reply(MessageBuilder.json()
                .image(player.getName(), HologramUtil.imageToBase64(inventoryImage))
                .build());
    }

    @EventHandler
    public void onOwnEndChest(MessageEvent event) throws Exception {
        final Player player = keyCheckAndGetOnlinePlayer(myEnder, event);
        if (player == null) return;
        ICPlayer icSender = ICPlayerFactory.getICPlayer(player);
        final BufferedImage inventoryImage = ImageGeneration.getInventoryImage(player.getEnderChest(), icSender);
        event.reply(MessageBuilder.json()
                .image(player.getName(), HologramUtil.imageToBase64(inventoryImage))
                .build());
    }

    @Nullable
    private Player keyCheckAndGetOnlinePlayer(@NotNull String keyword, MessageEvent event) {
        final String message = event.getRawMessage();
        if (message.length() != keyword.length() || !keyword.equals(message)) return null;
        event.setCancelled(true); // cancel event
        final PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        String uuid;
        if (data == null || (uuid = data.getPreferUUID()) == null) {
            event.reply(language.get("unbind"));
            return null;
        }
        final OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        if (player.getPlayer() == null || !player.isOnline()) {
            event.reply(language.get("offline").replace("%player_name%", player.getName() == null ? "null" : player.getName()));
            return null;
        }
        return player.getPlayer();
    }

}
