package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.illtamer.infinite.bot.expansion.manager.basic.listener.keywords.OnGlobalCmdListener;
import com.illtamer.infinite.bot.expansion.manager.basic.listener.keywords.OnLoginOutListener;
import com.illtamer.infinite.bot.expansion.manager.basic.listener.keywords.OnShowPlayersListener;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.SimpleDateFormat;
import java.util.*;

public class KeyWordsListener implements Listener {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final String newPlayer;
    private final String myBind;

    private final Language language;
    private final FileConfiguration config;

    public KeyWordsListener(ExpansionConfig configFile, Language language, IExpansion expansion) {
        this.config = configFile.getConfig();
        final ConfigurationSection section = config.getConfigurationSection("key-word");
        this.newPlayer = section.getString("new-player");
        this.myBind = section.getString("my-bind");
        this.language = language;

        new OnLoginOutListener(configFile, language, expansion).register();
        new OnShowPlayersListener(configFile, language, expansion).register();
        new OnGlobalCmdListener(configFile, language, expansion).register();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDailyPlayer(MessageEvent event) {
        if (!StaticAPI.isAdmin(event.getUserId()) || StringUtil.isBlank(newPlayer) || !newPlayer.equals(event.getRawMessage())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskAsynchronously(BukkitBootstrap.getInstance(), () -> {
            int count = 0;
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (equals(new Date(System.currentTimeMillis()),new Date(player.getFirstPlayed()))) {
                    count ++;
                }
            }
            event.reply(String.format(language.get("key-word", "new-player"), count));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCheckBind(MessageEvent event) {
        if (StringUtil.isBlank(myBind) || !myBind.equals(event.getRawMessage())) {
            return;
        }
        event.setCancelled(true);
        PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || (data.getUuid() == null && data.getValidUUID() == null)) {
            event.reply(language.get("key-word", "unchecked"));
        } else {
            final MessageBuilder builder = MessageBuilder.json();
            if (data.getUuid() != null) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid()));
                builder.text(String.format(
                        language.get("key-word", "bind-offline"),
                        player.getName(),
                        player.isOnline() ? "(在线)" : ("(离线)\n最后一次登录: " + FORMAT.format(new Date(player.getLastPlayed())))
                ));
            }
            if (data.getValidUUID() != null) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(data.getValidUUID()));
                builder.text(String.format(
                        language.get("key-word", "bind-valid"),
                        player.getName(),
                        player.isOnline() ? "(在线)" : ("(离线)\n最后一次登录: " + FORMAT.format(new Date(player.getLastPlayed())))
                ));
            }
            event.reply(builder.build());
        }
    }

    private static boolean equals(Date date1, Date date2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(date1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(date2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

}
