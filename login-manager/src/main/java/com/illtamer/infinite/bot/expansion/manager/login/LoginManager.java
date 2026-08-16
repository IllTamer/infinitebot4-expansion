package com.illtamer.infinite.bot.expansion.manager.login;

import com.illtamer.infinite.bot.expansion.manager.login.entity.PendingRequest;
import com.illtamer.infinite.bot.expansion.manager.login.listener.GameListener;
import com.illtamer.infinite.bot.expansion.manager.login.listener.QQListener;
import com.illtamer.infinite.bot.expansion.manager.login.service.AuthMeBridge;
import com.illtamer.infinite.bot.expansion.manager.login.service.FriendCache;
import com.illtamer.infinite.bot.expansion.manager.login.service.IPLocationService;
import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * LoginManager 主类
 * <p>
 * 集成 AuthMe：玩家进服后被 AuthMe 判定为待登录时，
 * 若玩家绑定的 QQ 为机器人好友，则通过私聊发送验证请求，
 * 玩家回复"确认"后由 AuthMe 免密登录。
 * 未绑定 QQ 或非机器人好友的玩家不触发校验，走 AuthMe 正常密码流程。
 * */
@Getter
public class LoginManager extends InfiniteExpansion {

    @Getter
    private static LoginManager instance;

    private ExpansionConfig configFile;
    private Language language;
    private AuthMeBridge authMeBridge;
    private FriendCache friendCache;
    private IPLocationService ipLocationService;

    /**
     * qq -> 待验证请求
     * */
    private final Map<Long, PendingRequest> pending = new ConcurrentHashMap<>();

    /**
     * 待验证请求的超时兜底（毫秒），仅用于清理过期旧请求，与允许登录无关
     * */
    private long noticeTimeoutMillis;

    private ScheduledFuture<?> cleanupTask;

    @Override
    public void onEnable() {
        instance = this;
        this.configFile = new ExpansionConfig("config.yml", this, 2);
        this.language = Language.of(this);

        final FileConfiguration config = configFile.getConfig();
        this.noticeTimeoutMillis = Math.max(30L, config.getLong("notice-timeout", 300L)) * 1000L;

        this.authMeBridge = new AuthMeBridge();

        long friendCacheSeconds = Math.max(30L, config.getLong("friend-cache", 300L));
        this.friendCache = new FriendCache(friendCacheSeconds);

        this.ipLocationService = new IPLocationService(
                config.getBoolean("ip-api.enable", true),
                config.getString("ip-api.url", "https://ip.useragentinfo.com/json?ip=%ip%"),
                config.getString("ip-api.field.province", "province"),
                config.getString("ip-api.field.city", "city"),
                config.getString("ip-api.field.isp", "isp"),
                config.getLong("ip-api.cache", 300L),
                config.getBoolean("skip-local-ip", true),
                language.get("location", "local"),
                language.get("location", "unknown")
        );

        EventExecutor.registerBukkitEvent(new GameListener(this), this);
        EventExecutor.registerEvents(new QQListener(this), this);

        this.cleanupTask = BotScheduler.runTaskTimer(this::cleanup, 20L, 20L);
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null) {
            try {
                cleanupTask.cancel(false);
            } catch (Exception ignore) {}
            cleanupTask = null;
        }
        pending.clear();
        instance = null;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(entry -> entry.getValue().getExpireAt() <= now);
        if (ipLocationService != null) {
            ipLocationService.cleanup();
        }
    }

    @Override
    public String getExpansionName() {
        return "LoginManager";
    }

    @Override
    public String getVersion() {
        return "1.1";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

}
