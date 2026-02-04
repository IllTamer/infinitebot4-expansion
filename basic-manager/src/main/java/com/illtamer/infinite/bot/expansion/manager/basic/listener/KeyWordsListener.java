package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.illtamer.infinite.bot.expansion.manager.basic.distribute.*;
import com.illtamer.infinite.bot.expansion.manager.basic.pojo.DataOnShowPlayers;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import com.illtamer.perpetua.sdk.entity.transfer.entity.Client;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class KeyWordsListener implements Listener {

    private static final Logger logger = LoggerFactory.getLogger(KeyWordsListener.class);
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final String newPlayer;
    private final String myBind;
    private final String loginOut;
    private final String online;
    private final Language language;
    private final FileConfiguration config;
    private final DistributedEventProcessor<DataOnShowPlayers> showPlayersProcessor;
    private final DistributedEventProcessor<LoginOutData> loginOutProcessor;
    public static final String EVENT_KEY_SHOW_PLAYERS = "showPlayers";
    public static final String EVENT_KEY_LOGIN_OUT = "loginOut";

    public KeyWordsListener(ExpansionConfig configFile, Language language, IExpansion expansion) {
        this.config = configFile.getConfig();
        final ConfigurationSection section = config.getConfigurationSection("key-word");
        this.newPlayer = section.getString("new-player");
        this.myBind = section.getString("my-bind");
        this.loginOut = section.getString("login-out");
        this.online = section.getString("online");
        this.language = language;
        
        // 初始化分布式事件处理器
        this.showPlayersProcessor = new DistributedEventProcessor<>(expansion, DataOnShowPlayers.class);
        this.loginOutProcessor = new DistributedEventProcessor<>(expansion, LoginOutData.class);
        
        // 注册事件处理器
        registerEventHandlers(expansion);
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

        // 创建事件上下文
        DistributedEventContext context = new DistributedEventContext();
        context.setParam("userId", event.getUserId());
        context.setParam("sender", event.getSender());

        loginOutProcessor.tryProcessEvent(EVENT_KEY_LOGIN_OUT, context, result -> {
            // 汇总结果并回复
            if (result.isAllSuccess()) {
                // 所有客户端执行成功
                StringBuilder reply = new StringBuilder(language.get("key-word", "kick-success"));
                for (LoginOutData data : result.getDataList()) {
                    if (!data.getKickedPlayers().isEmpty()) {
                        reply.append("\n").append(data.getClientName()).append(": ")
                             .append(String.join(", ", data.getKickedPlayers()));
                    }
                }
                event.reply(reply.toString());
            } else if (result.isPartialSuccess()) {
                // 部分成功
                event.reply(language.get("key-word", "partial-success"));
            } else {
                // 全部失败
                event.reply(language.get("key-word", "offline"));
            }
        }, e -> {
            logger.error("登录登出分布式事件处理异常", e);
            event.reply(language.get("key-word", "error"));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShowPlayers(GroupMessageEvent event) {
        if (!online.equals(event.getRawMessage())) {
            return;
        }
        event.setCancelled(true);

        // 创建事件上下文
        DistributedEventContext context = new DistributedEventContext();

        showPlayersProcessor.tryProcessEvent(EVENT_KEY_SHOW_PLAYERS, context, result -> {
            List<DataOnShowPlayers> dataList = result.getDataList();
            List<Client> failedClientList = result.getFailedClientList();

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
        }, e -> {
            logger.error("显示玩家分布式事件处理异常", e);
            event.reply(language.get("key-word", "error"));
        });
    }

    /**
     * 注册事件处理器
     */
    private void registerEventHandlers(IExpansion expansion) {
        // 注册显示玩家事件处理器
        showPlayersProcessor.registerHandler(EVENT_KEY_SHOW_PLAYERS, context -> {
            return getPlayerListData();
        });
        
        // 注册登录登出事件处理器
        loginOutProcessor.registerHandler(EVENT_KEY_LOGIN_OUT, context -> {
            String userId = context.getParam("userId");
            LoginOutData result = new LoginOutData(StaticAPI.getClient().getClientName());
            
            PlayerData data = StaticAPI.getRepository().queryByUserId(Long.parseLong(userId));
            if (data == null || (data.getUuid() == null && data.getValidUUID() == null)) {
                return result;
            }

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
                Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> {
                    players.forEach(player -> {
                        player.kickPlayer(PluginUtil.parseColor(language.get("key-word", "kick")));
                        result.addKickedPlayer(player.getName());
                    });
                });
            }
            
            return result;
        });
        
        // 注册监听器
        expansion.registerListener(showPlayersProcessor.createListener());
        expansion.registerListener(loginOutProcessor.createListener());
    }

    /**
     * 获取玩家列表数据
     */
    public static DataOnShowPlayers getPlayerListData() {
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
