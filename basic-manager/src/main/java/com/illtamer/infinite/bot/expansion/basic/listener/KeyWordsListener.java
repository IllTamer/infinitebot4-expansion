package com.illtamer.infinite.bot.expansion.basic.listener;

import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class KeyWordsListener implements Listener {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final String newPlayer;
    private final String myBind;
    private final String loginOut;
    private final String online;
    private final Language language;

    public KeyWordsListener(ExpansionConfig config, Language language) {
        final ConfigurationSection section = config.getConfig().getConfigurationSection("key-word");
        this.newPlayer = section.getString("new-player");
        this.myBind = section.getString("my-bind");
        this.loginOut = section.getString("login-out");
        this.online = section.getString("online");
        this.language = language;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDailyPlayer(MessageEvent event) {
        if (!StaticAPI.isAdmin(event.getUserId()) || !newPlayer.equals(event.getRawMessage())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> {
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
        if (!myBind.equals(event.getRawMessage())) {
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginOut(MessageEvent event) {
        if (!loginOut.equals(event.getRawMessage())) {
            return;
        }
        event.setCancelled(true);
        PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || (data.getUuid() == null && data.getValidUUID() == null)) {
            event.reply(language.get("key-word", "unchecked"));
        } else {
            List<Player> players = new ArrayList<>(2);
            if (data.getUuid() != null) {
                final Player player = Lambda.nullableInvoke(OfflinePlayer::getPlayer, Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid())));
                if (player != null) players.add(player);
            }
            if (data.getValidUUID() != null) {
                final Player player = Lambda.nullableInvoke(OfflinePlayer::getPlayer, Bukkit.getOfflinePlayer(UUID.fromString(data.getValidUUID())));
                if (player != null) players.add(player);
            }
            if (players.size() != 0) {
                Bukkit.getScheduler().runTask(Bootstrap.getInstance(), () -> players.forEach(player ->
                        player.kickPlayer(PluginUtil.parseColor(language.get("key-word", "kick").replace("%qq%", event.getSender().getUserId().toString())))
                ));
                event.reply(language.get("key-word", "kick-success"));
            } else {
                event.reply(language.get("key-word", "offline"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShowPlayers(GroupMessageEvent event) {
        if (!online.equals(event.getRawMessage())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> {
            Collection<? extends Player> players = Bootstrap.getInstance().getServer().getOnlinePlayers();
            Iterator<? extends Player> iterator = players.iterator();
            if (!iterator.hasNext()) {
                event.reply(language.get("key-word", "no-player"));
                return;
            }
            String opPrefix = "\n管理员：", playerPrefix = "\n玩家：";
            StringBuilder opList = new StringBuilder(opPrefix);
            StringBuilder playerList = new StringBuilder(playerPrefix);
            while (iterator.hasNext()) {
                Player player = iterator.next();
                if (player.isOp()) {
                    opList.append(PluginUtil.clearColor(player.getDisplayName())).append(", ");
                } else {
                    playerList.append(PluginUtil.clearColor(player.getDisplayName())).append(", ");
                }
            }
            String format = String.format(language.get("key-word", "show-player"),
                    players.size(),
                    opList.length() == opPrefix.length() ? "" : opList.toString(),
                    playerList.length() == playerPrefix.length() ? "" : playerList.toString());
            event.reply(format);
        });
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
