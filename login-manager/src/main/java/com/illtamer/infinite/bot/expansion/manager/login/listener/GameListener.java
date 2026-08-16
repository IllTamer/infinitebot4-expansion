package com.illtamer.infinite.bot.expansion.manager.login.listener;

import com.illtamer.infinite.bot.expansion.manager.login.LoginManager;
import com.illtamer.infinite.bot.expansion.manager.login.entity.IPLocation;
import com.illtamer.infinite.bot.expansion.manager.login.entity.PendingRequest;
import com.illtamer.infinite.bot.expansion.manager.login.service.AuthMeBridge;
import com.illtamer.infinite.bot.expansion.manager.login.service.FriendCache;
import com.illtamer.infinite.bot.expansion.manager.login.service.IPLocationService;
import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家进服 / 退服监听
 * <p>
 * 玩家进入服务器并被 AuthMe 判定为待登录时，向玩家绑定的 QQ 发送验证消息；
 * 玩家回复确认后由 {@link QQListener} 完成免密登录。
 * */
public class GameListener implements Listener {

    private final LoginManager plugin;
    private final Language language;
    private final AuthMeBridge authMeBridge;
    private final FriendCache friendCache;
    private final IPLocationService ipLocationService;
    private final Map<Long, PendingRequest> pending;
    private final long noticeTimeoutMillis;

    public GameListener(LoginManager plugin) {
        this.plugin = plugin;
        this.language = plugin.getLanguage();
        this.authMeBridge = plugin.getAuthMeBridge();
        this.friendCache = plugin.getFriendCache();
        this.ipLocationService = plugin.getIpLocationService();
        this.pending = plugin.getPending();
        this.noticeTimeoutMillis = plugin.getNoticeTimeoutMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        // AuthMe 未启用时不触发任何逻辑
        if (!authMeBridge.isEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // AuthMe 未要求该玩家输入密码（已认证 / 会话恢复）则跳过
        if (!authMeBridge.isPendingAuth(player)) return;

        // 未绑定 QQ 直接跳过
        PlayerData data = StaticAPI.getRepository().queryByUUID(uuid);
        if (data == null || data.getUserId() == null || data.getUserId() == 0L) return;
        long qq = data.getUserId();

        // 非机器人好友直接跳过
        if (!friendCache.isFriend(qq)) return;

        InetSocketAddress socket = player.getAddress();
        String ip = socket == null || socket.getAddress() == null ? "" : socket.getAddress().getHostAddress();
        IPLocation location = ipLocationService.query(ip);

        long expireAt = System.currentTimeMillis() + noticeTimeoutMillis;
        pending.put(qq, new PendingRequest(player.getName(), uuid, expireAt));

        // 异步发送 QQ 私聊，避免阻塞 Bukkit 主线程
        final long targetQq = qq;
        final String playerName = player.getName();
        BotScheduler.runTask(() -> sendNotice(targetQq, playerName, location));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = StaticAPI.getRepository().queryByUUID(uuid);
        if (data != null && data.getUserId() != null) {
            pending.remove(data.getUserId());
        }
    }

    private void sendNotice(long qq, String playerName, IPLocation location) {
        String text = language.get("message", "qq-notice")
                .replace("%player%", playerName)
                .replace("%province%", nullSafe(location.getProvince()))
                .replace("%city%", nullSafe(location.getCity()))
                .replace("%isp%", nullSafe(location.getIsp()));
        try {
            OpenAPIHandling.sendMessage(text, qq);
        } catch (Exception e) {
            // 发送失败不影响 AuthMe 正常流程，玩家仍可使用密码登录
            plugin.getLogger().warn("发送 QQ 验证私聊失败 (qq=" + qq + "): " + e.getMessage());
        }
    }

    private String nullSafe(String v) {
        return v == null ? language.get("location", "unknown") : v;
    }

}
