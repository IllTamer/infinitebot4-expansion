package com.illtamer.infinite.bot.expansion.defence.listener;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.expansion.defence.DefenceManager;
import com.illtamer.infinite.bot.expansion.defence.entity.AuthData;
import com.illtamer.infinite.bot.expansion.defence.util.AuthUtil;
import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.repository.PlayerDataRepository;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.infinite.bot.minecraft.util.ValidUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

public class LoginListener implements Listener {
    private static final HashMap<UUID, AuthData> DATA_HASH_MAP = new HashMap<>();
    private static final LinkedList<ScheduledFuture<?>> authList = new LinkedList<>();
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final HashMap<String, Integer> ATTACKERS = new HashMap<>();
    private static ScheduledFuture<?> SCHEDULED;
    private static int joins;

//    private final boolean authme;
    private final List<String> kickMessages;
    private final boolean defence;
    private final boolean tips;
    private final int threshold;
    private final int limit;
    private final boolean whiteList;
    private final List<String> access;
    private final String accessPrefix;

    public LoginListener(ExpansionConfig configFile) {
        final FileConfiguration config = configFile.getConfig();
//        this.authme = config.getBoolean("auth-me");
        this.whiteList = config.getBoolean("white-list");
        this.access = config.getStringList("access");
        this.accessPrefix = config.getString("access-prefix");
        this.limit = config.getInt("limit");
        this.kickMessages = config.getStringList("messages");
        this.defence = config.getBoolean("defence.enable");
        this.tips = config.getBoolean("defence.tips");
        this.threshold = config.getInt("defence.threshold");
        if (DATA_HASH_MAP.size() != 0) {
            DATA_HASH_MAP.clear();
        }
        SCHEDULED = BotScheduler.runTaskTimer(() -> {
            if (joins > 0) -- joins;
            if (authList.size() > 256) {
                DefenceManager.getInstance().getLogger().warn("短时间内验证人数过多(" + authList.size() + "次)! 自动清理缓存数据");
                for (Iterator<ScheduledFuture<?>> iterator = authList.iterator(); iterator.hasNext(); ) {
                    try {
                        iterator.next().cancel(true);
                        iterator.remove();
                    } catch (Exception ignore) {}
                }
            }
        }, 5, 1);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
//        if (authme && AuthMeApi.getInstance().isRegistered(event.getName())) return;
        if (whiteList) {
            for (OfflinePlayer offlinePlayer : Bukkit.getWhitelistedPlayers()) {
                if (offlinePlayer.getUniqueId().equals(event.getUniqueId())) return;
            }
        }
        if (access.contains(event.getName())) return;
        if (accessPrefix != null && accessPrefix.length() > 0 && event.getName().startsWith(accessPrefix)) return;
        final PlayerDataRepository repository = StaticAPI.getRepository();
        PlayerData data = repository.queryByUUID(event.getUniqueId());
        if (data != null) return;
        UUID uuid = event.getUniqueId();
        // 检查缓存
        if (DATA_HASH_MAP.containsKey(uuid)) {
            if (repository.queryByUUID(uuid) != null) { // 清除失败？
                DATA_HASH_MAP.remove(uuid);
                return;
            }
            AuthData authData = DATA_HASH_MAP.get(uuid);
            kick(authData.getCode(), authData.getTime(), kickMessages, event);
            return;
        }

        final String time = FORMAT.format(new Date(System.currentTimeMillis() + limit * 60000L));
        final String code = AuthUtil.getCode(joins);
        final boolean valid = ValidUtil.isValidUUID(uuid);
        AuthData authData = new AuthData(code, time, valid);
        DATA_HASH_MAP.put(uuid, authData);

        authList.add(BotScheduler.runTaskLater(() ->
                DATA_HASH_MAP.remove(uuid), limit * 60L)
        );
        kick(code, time, kickMessages, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDefence(AsyncPlayerPreLoginEvent event) {
        if (!defence) return;
        if (++ joins <= 64) return;

        String address = event.getAddress().getHostAddress();
        int now = 1;
        if (ATTACKERS.containsKey(address)) {
            now = ATTACKERS.get(address) + 1;
        }
        ATTACKERS.put(address, now);
        if (now < threshold) return;
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,tips ? "§c§l检测非法连接，已将您踢出服务器！" : "");
        ATTACKERS.put(address,++now);
    }

    /**
     * 清空攻击次数小于一定值的攻击者
     * */
    public static void clearCommon(int threshold) {
        ATTACKERS.entrySet().removeIf(entry -> entry.getValue().compareTo(threshold) <= 0);
    }

    public static void unregisterTask() {
        try {
            SCHEDULED.cancel(true);
        } catch (Exception ignore) {}
    }

    @Nullable
    public static Pair<UUID, AuthData> getByCode(String code) {
        for (Map.Entry<UUID, AuthData> entry : DATA_HASH_MAP.entrySet()) {
            if (entry.getValue().getCode().equalsIgnoreCase(code)) {
                return new Pair<>(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }

    public static boolean removeByUUID(UUID uuid) {
        return DATA_HASH_MAP.remove(uuid) != null;
    }

    private static void kick(String code, String time, List<String> kickMessages, AsyncPlayerPreLoginEvent event) {
        StringBuilder builder = new StringBuilder();
        for (String temp : kickMessages) {
            builder.append(temp
                    .replace("%code%", code)
                    .replace("%time%", time)
                    .replace("%player%", event.getName())
            ).append("\n");
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, PluginUtil.parseColor(builder.toString()));
    }

}
