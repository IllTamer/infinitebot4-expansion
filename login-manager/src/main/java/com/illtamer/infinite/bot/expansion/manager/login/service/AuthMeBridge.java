package com.illtamer.infinite.bot.expansion.manager.login.service;

import com.illtamer.infinite.bot.expansion.manager.login.LoginManager;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * AuthMe 桥接层
 * <p>
 * 对 {@link AuthMeApi} 的薄封装，便于在 AuthMe 未安装 / 未启用时安全降级。
 * */
public class AuthMeBridge {

    private final boolean enabled;

    public AuthMeBridge() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("AuthMe");
        if (!enabled) {
            LoginManager instance = LoginManager.getInstance();
            if (instance != null) {
                instance.getLogger().warn("未检测到 AuthMe 插件，login-manager 将跳过登录验证逻辑");
            }
        }
    }

    /**
     * AuthMe 是否可用（插件已启用且 API 已初始化）
     * */
    public boolean isEnabled() {
        return enabled && AuthMeApi.getInstance() != null;
    }

    /**
     * 玩家当前是否处于 AuthMe 待登录（未认证）状态
     * */
    public boolean isPendingAuth(Player player) {
        AuthMeApi api = AuthMeApi.getInstance();
        return api != null && !api.isAuthenticated(player);
    }

    /**
     * 直接免密登录玩家
     * <p>
     * 必须在 Bukkit 主线程调用，调用前请确认玩家在线。
     * */
    public void forceLogin(Player player) {
        AuthMeApi api = AuthMeApi.getInstance();
        if (api == null) {
            return;
        }
        api.forceLogin(player);
    }

}
