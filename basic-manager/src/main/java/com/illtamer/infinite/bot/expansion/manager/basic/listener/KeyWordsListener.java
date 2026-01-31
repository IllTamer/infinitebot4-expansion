package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.google.gson.Gson;
import com.illtamer.infinite.bot.expansion.manager.basic.BasicManager;
import com.illtamer.infinite.bot.expansion.manager.basic.pojo.DataOnShowPlayers;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.pojo.TimedBlockingCache;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import com.illtamer.perpetua.sdk.Pair;
import com.illtamer.perpetua.sdk.entity.transfer.entity.Client;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class KeyWordsListener implements Listener {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final String newPlayer;
    private final String myBind;
    private final String loginOut;
    private final String online;
    private final Language language;
    private final FileConfiguration config;
    private final DistributeHelper distributeHelper;
    public static final String ON_SHOW_PLAYERS = "onShowPlayers#";
    public static final String ON_LOGIN_OUT = "onLoginOut#";

    public KeyWordsListener(ExpansionConfig configFile, Language language, DistributeHelper distributeHelper) {
        this.config = configFile.getConfig();
        final ConfigurationSection section = config.getConfigurationSection("key-word");
        this.newPlayer = section.getString("new-player");
        this.myBind = section.getString("my-bind");
        this.loginOut = section.getString("login-out");
        this.online = section.getString("online");
        this.language = language;
        this.distributeHelper = distributeHelper;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDailyPlayer(MessageEvent event) {
        if (!StaticAPI.isAdmin(event.getUserId()) || !newPlayer.equals(event.getRawMessage())) {
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

        distributeHelper.tryHandle(context -> {

        }, null, ON_LOGIN_OUT, Boolean.class);

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
            if (!players.isEmpty()) {
                Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> players.forEach(player ->
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

        distributeHelper.tryHandle(context -> {
            List<DataOnShowPlayers> dataList = context.getDataList();
            List<Client> failedClientList = context.getFailedClientList();

            int opTotal = 0, playerTotal = 0;
            Map<String, Set<String>> serverOpMap = new HashMap<>();
            Map<String, Set<String>> serverPlayerMap = new HashMap<>();
            for (DataOnShowPlayers data : dataList) {
                String clientName = StringUtil.isBlank(data.getClientName()) ? "未命名客户端" : data.getClientName();
                serverOpMap.computeIfAbsent(clientName, k -> new HashSet<>()).addAll(data.getOpList());
                serverPlayerMap.computeIfAbsent(clientName, k -> new HashSet<>()).addAll(data.getPlayerList());
                opTotal += data.getOpList().size();
                playerTotal += data.getPlayerList().size();
            }

            if (serverOpMap.isEmpty() && serverPlayerMap.isEmpty()) {
                event.reply(language.get("key-word", "no-player"));
                return;
            }

            // - [clientName1]
            //   - op1, op2
            StringBuilder opStr = new StringBuilder();
            if (opTotal > 0 && config.getBoolean("online.show-op")) {
                opStr.append("\n管理员: ");
                serverOpMap.forEach((key, value) -> {
                    if (!value.isEmpty()) {
                        opStr.append("\n- ").append(key)
                                .append("\n  - ").append(String.join(", ", value));
                    }
                });
            }
            StringBuilder playerStr = new StringBuilder();
            if (playerTotal > 0) {
                playerStr.append("\n玩家: ");
                serverPlayerMap.forEach((key, value) -> {
                    if (!value.isEmpty()) {
                        playerStr.append("\n- ").append(key)
                                .append("\n  - ").append(String.join(", ", value));
                    }
                });
            }

            String msgBuilder = "服务器当前总人数: " + (opTotal + playerTotal) + opStr + playerStr +
                    (failedClientList.isEmpty() ? "" : "\n访问超时的客户端数: " + failedClientList.size());
            event.reply(msgBuilder);
        }, null, ON_SHOW_PLAYERS, DataOnShowPlayers.class);
    }

    // {"opList": [], "playerList": [], "clientName": ""}
    public static DataOnShowPlayers getPlayerListJson() {
        DataOnShowPlayers data = new DataOnShowPlayers();
        data.setClientName(StaticAPI.getClient().getClientName());
        Collection<? extends Player> players = BukkitBootstrap.getInstance().getServer().getOnlinePlayers();
        if (players.isEmpty()) {
            return data;
        }

        for (Player player : players) {
            if (player.isOp()) {
                data.getOpList().add(PluginUtil.clearColor(player.getDisplayName()));
            } else {
                data.getPlayerList().add(PluginUtil.clearColor(player.getDisplayName()));
            }
        }
        return data;
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
