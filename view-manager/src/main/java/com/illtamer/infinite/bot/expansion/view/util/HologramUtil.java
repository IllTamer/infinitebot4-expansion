package com.illtamer.infinite.bot.expansion.view.util;

import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.api.InteractiveChatAPI;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.Component;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.minimessage.MiniMessage;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.loohp.interactivechat.objectholders.*;
import com.loohp.interactivechat.utils.*;
import com.loohp.interactivechatdiscordsrvaddon.InteractiveChatDiscordSrvAddon;
import com.loohp.interactivechatdiscordsrvaddon.graphics.ImageGeneration;
import com.loohp.interactivechatdiscordsrvaddon.listeners.DiscordCommands;
import com.loohp.interactivechatdiscordsrvaddon.utils.DiscordItemStackUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HologramUtil {

    private static final String PREFIX = "base64://";
    private static final boolean UPPER = PluginUtil.Version.upper(12);

    private HologramUtil() {}

    public static BufferedImage getTooltip(ItemStack item, Player player) {
        BufferedImage tooltip = null;
        try {
            final Material type = item.getType();
            tooltip = new BufferedImage(8, 8, 6);
            if (item.hasItemMeta() && item.getItemMeta() instanceof MapMeta) {
                tooltip = ImageGeneration.getMapImage(item, player).get();
            } else {
                if (UPPER) {
                    if (item.getItemMeta() != null && type == Material.SHULKER_BOX) {
                        ShulkerBox box = (ShulkerBox) item.getItemMeta();
                        return ImageGeneration.getInventoryImage(box.getInventory(), ICPlayerFactory.getICPlayer(player));
                    }
                }
                tooltip = ImageGeneration.getToolTipImage(DiscordItemStackUtils.getToolTip(item, ICPlayerFactory.getICPlayer(player)).getComponents());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tooltip;
    }

    public static BufferedImage getPlayerListImage() {
        try {
            Map<OfflinePlayer, Integer> players;
            if (InteractiveChat.bungeecordMode && InteractiveChatDiscordSrvAddon.plugin.playerlistCommandBungeecord && !Bukkit.getOnlinePlayers().isEmpty()) {
                try {
                    List<ValueTrios<UUID, String, Integer>> bungeePlayers = InteractiveChatAPI.getBungeecordPlayerList().get();
                    players = new LinkedHashMap<>(bungeePlayers.size());
                    for (ValueTrios<UUID, String, Integer> playerinfo : bungeePlayers) {
                        UUID uuid = playerinfo.getFirst();
                        ICPlayer icPlayer = ICPlayerFactory.getICPlayer(uuid);
                        if (icPlayer == null || !icPlayer.isVanished()) {
                            if (!InteractiveChatDiscordSrvAddon.plugin.playerlistCommandOnlyInteractiveChatServers || ICPlayerFactory.getICPlayer(uuid) != null) {
                                players.put(Bukkit.getOfflinePlayer(uuid), playerinfo.getThird());
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                players = Bukkit.getOnlinePlayers().stream().filter(each -> {
                    ICPlayer icPlayer = ICPlayerFactory.getICPlayer(each);
                    return icPlayer == null || !icPlayer.isVanished();
                }).collect(Collectors.toMap(each -> each, PlayerUtils::getPing, (a, b) -> a));
            }
            List<ValueTrios<OfflineICPlayer, Component, Integer>> player = new ArrayList<>();
            Map<UUID, ValuePairs<List<String>, String>> playerInfo = new HashMap<>();
            for (Map.Entry<OfflinePlayer, Integer> entry : players.entrySet()) {
                OfflinePlayer bukkitOfflinePlayer = entry.getKey();
                @SuppressWarnings("deprecation")
                OfflineICPlayer offlinePlayer = ICPlayerFactory.getUnsafe().getOfflineICPPlayerWithoutInitialization(bukkitOfflinePlayer.getUniqueId());
                playerInfo.put(offlinePlayer.getUniqueId(), new ValuePairs<>(DiscordCommands.getPlayerGroups(bukkitOfflinePlayer), offlinePlayer.getName()));
                String name = ChatColorUtils.translateAlternateColorCodes('&', PlaceholderParser.parse(offlinePlayer, InteractiveChatDiscordSrvAddon.plugin.playerlistCommandPlayerFormat));
                Component nameComponent;
                if (InteractiveChatDiscordSrvAddon.plugin.playerlistCommandParsePlayerNamesWithMiniMessage) {
                    nameComponent = MiniMessage.miniMessage().deserialize(name);
                } else {
                    nameComponent = InteractiveChatComponentSerializer.legacySection().deserialize(name);
                }
                player.add(new ValueTrios<>(offlinePlayer, nameComponent, entry.getValue()));
            }
            DiscordCommands.sortPlayers(InteractiveChatDiscordSrvAddon.plugin.playerlistOrderingTypes, player, playerInfo);
            @SuppressWarnings("deprecation")
            OfflineICPlayer firstPlayer = ICPlayerFactory.getUnsafe().getOfflineICPPlayerWithoutInitialization(players.keySet().iterator().next().getUniqueId());
            List<Component> header = new ArrayList<>();
            if (!InteractiveChatDiscordSrvAddon.plugin.playerlistCommandHeader.isEmpty()) {
                header = ComponentStyling.splitAtLineBreaks(LegacyComponentSerializer.legacySection().deserialize(ChatColorUtils.translateAlternateColorCodes('&', PlaceholderParser.parse(firstPlayer, InteractiveChatDiscordSrvAddon.plugin.playerlistCommandHeader.replace("{OnlinePlayers}", players.size() + "")))));
            }
            List<Component> footer = new ArrayList<>();
            if (!InteractiveChatDiscordSrvAddon.plugin.playerlistCommandFooter.isEmpty()) {
                footer = ComponentStyling.splitAtLineBreaks(LegacyComponentSerializer.legacySection().deserialize(ChatColorUtils.translateAlternateColorCodes('&', PlaceholderParser.parse(firstPlayer, InteractiveChatDiscordSrvAddon.plugin.playerlistCommandFooter.replace("{OnlinePlayers}", players.size() + "")))));
            }
            int playerListMaxPlayers = InteractiveChatDiscordSrvAddon.plugin.playerlistMaxPlayers;
            if (playerListMaxPlayers < 1) {
                playerListMaxPlayers = Integer.MAX_VALUE;
            }
            return ImageGeneration.getTabListImage(header, footer, player, InteractiveChatDiscordSrvAddon.plugin.playerlistCommandAvatar, InteractiveChatDiscordSrvAddon.plugin.playerlistCommandPing, playerListMaxPlayers);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String imageToBase64(BufferedImage image) {
        return streamToBase64(output -> {
            try {
                ImageIO.write(image, "png", output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static String streamToBase64(Consumer<ByteArrayOutputStream> outputConsumer) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        outputConsumer.accept(output);
        return PREFIX + Base64.getEncoder().encodeToString(output.toByteArray());
    }
}
