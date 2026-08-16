package com.illtamer.infinite.bot.expansion.manager.login.listener;

import com.illtamer.infinite.bot.expansion.manager.login.LoginManager;
import com.illtamer.infinite.bot.expansion.manager.login.entity.PendingRequest;
import com.illtamer.infinite.bot.expansion.manager.login.service.AuthMeBridge;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.perpetua.sdk.event.message.PrivateMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * QQ 私聊回复监听
 * <p>
 * 玩家 QQ 私聊回复"确认"后，调用 AuthMe 免密登录该玩家。
 * */
public class QQListener implements Listener {

    private static final String CONFIRM_KEYWORD = "确认";

    private final LoginManager plugin;
    private final Language language;
    private final AuthMeBridge authMeBridge;
    private final Map<Long, PendingRequest> pending;

    public QQListener(LoginManager plugin) {
        this.plugin = plugin;
        this.language = plugin.getLanguage();
        this.authMeBridge = plugin.getAuthMeBridge();
        this.pending = plugin.getPending();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrivate(PrivateMessageEvent event) {
        // 仅处理好友私聊，忽略群临时会话
        if (!"friend".equals(event.getSubType())) return;

        String raw = event.getRawMessage();
        if (raw == null) return;
        String msg = raw.trim();
        if (!CONFIRM_KEYWORD.equals(msg)) return;

        long qq = event.getUserId();
        PendingRequest request = pending.remove(qq);
        if (request == null) {
            // 无待验证请求，静默处理，避免打扰其他私聊
            return;
        }

        long now = System.currentTimeMillis();
        if (request.getExpireAt() <= now) {
            event.setCancelled(true);
            event.reply(language.get("message", "reply-expired")
                    .replace("%player%", request.getPlayerName()));
            return;
        }

        event.setCancelled(true);
        final String playerName = request.getPlayerName();
        // forceLogin 需在 Bukkit 主线程执行
        Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> {
            Player player = Bukkit.getPlayer(request.getUuid());
            if (player == null || !player.isOnline()) {
                event.reply(language.get("message", "reply-offline")
                        .replace("%player%", playerName));
                return;
            }
            try {
                authMeBridge.forceLogin(player);
                event.reply(language.get("message", "reply-success")
                        .replace("%player%", playerName));
            } catch (Exception e) {
                plugin.getLogger().warn("AuthMe 免密登录失败 (player=" + playerName + "): " + e.getMessage());
                event.reply(language.get("message", "reply-offline")
                        .replace("%player%", playerName));
            }
        });
    }

}
