package com.illtamer.infinite.bot.expansion.mvg.skin;

import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * SkinsRestorer 软依赖钩子
 * <p>
 * <b>这是全项目唯一引用 {@code net.skinsrestorer.*} 的类。</b>
 * 附属没有安装 SkinsRestorer 时，{@link #init(Logger)} 不会触碰任何 SkinsRestorer 类型，
 * 类加载器不会去解析这些符号引用；即便插件存在但 API 未初始化，
 * {@link #resolve(UUID, String)} 也会捕获全部 {@link Throwable} 并降级。
 * <p>
 * 早期实现只在附属 {@code onEnable} 探测一次，探测不到就永久降级 —— 群组服里
 * SkinsRestorer 常只装在 Velocity 代理端、或后端插件比附属更晚初始化，
 * 那样会让所有玩家一直落到默认皮肤。因此这里改为<b>懒探测 + 节流重试</b>。
 * */
public final class SkinsRestorerHook {

    private static final String PLUGIN_NAME = "SkinsRestorer";

    /**
     * 探测失败后的重试间隔：避免每次解析都去尝试一次
     * */
    private static final long RETRY_INTERVAL_MILLIS = 30_000L;

    private static volatile boolean available;
    private static volatile long nextAttemptAt;
    private static volatile boolean informed;
    private static Logger logger;

    private SkinsRestorerHook() {}

    /**
     * 在附属 onEnable 中调用（此时 SkinsRestorer 可能还没初始化，失败不影响后续重试）
     * */
    public static void init(Logger expansionLogger) {
        logger = expansionLogger;
        available = false;
        nextAttemptAt = 0L;
        informed = false;
        probe(true);
    }

    /**
     * 是否可用；未就绪时会按节流间隔重新探测
     * */
    public static boolean isAvailable() {
        if (available) {
            return true;
        }
        return probe(false);
    }

    /**
     * 通过 SkinsRestorer 解析玩家皮肤的 texture value
     *
     * @return 解析出的贴图地址与模型，不可用时返回 {@code null}
     * */
    public static TextureValueParser.Result resolve(UUID uuid, String playerName) {
        if (!isAvailable() || uuid == null) {
            return null;
        }
        try {
            net.skinsrestorer.api.SkinsRestorer api = net.skinsrestorer.api.SkinsRestorerProvider.get();
            net.skinsrestorer.api.storage.PlayerStorage storage = api.getPlayerStorage();
            // 1. 玩家在 SkinsRestorer 内绑定的皮肤（只读本地存储，不发起网络请求）
            Optional<net.skinsrestorer.api.property.SkinProperty> property = storage.getSkinOfPlayer(uuid);
            // 2. 进服会应用的皮肤（自定义 / 正版 / 默认，可能触发 Mojang 查询）。
            //    onlineMode 必须如实传入：两参重载内部固定传 false，正版服上会拿到 default 皮肤
            if (property.isEmpty() && StringUtil.isNotBlank(playerName)) {
                property = storage.getSkinForPlayer(uuid, playerName, isOnlineMode());
            }
            if (property.isEmpty()) {
                return null;
            }
            return TextureValueParser.parse(property.get().getValue());
        } catch (Throwable e) {
            if (logger != null) {
                logger.debug("[Skin] 通过 " + PLUGIN_NAME + " 解析皮肤失败: " + uuid, e);
            }
            return null;
        }
    }

    /**
     * 服务器是否为正版验证模式（SkinsRestorer 用它决定是否跳过 default 皮肤）
     * */
    public static boolean isOnlineMode() {
        try {
            return Bukkit.getServer().getOnlineMode();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * @param verbose 是否输出"未接入"的日志（只在 onEnable 首次探测时输出）
     * @return 是否可用
     * */
    private static boolean probe(boolean verbose) {
        long now = System.currentTimeMillis();
        if (!verbose && now < nextAttemptAt) {
            return false;
        }
        nextAttemptAt = now + RETRY_INTERVAL_MILLIS;
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (plugin == null || !plugin.isEnabled()) {
                note(verbose, "[Skin] 未检测到 " + PLUGIN_NAME + " 插件，将依次尝试玩家档案与 Mojang 档案");
                return false;
            }
            Object api = net.skinsrestorer.api.SkinsRestorerProvider.get();
            if (api == null) {
                note(verbose, "[Skin] " + PLUGIN_NAME + " API 尚未初始化，暂用玩家档案与 Mojang 档案");
                return false;
            }
            available = true;
            nextAttemptAt = 0L;
            if (logger != null && !informed) {
                logger.info("[Skin] 已接入 " + PLUGIN_NAME + " API，皮肤优先从这里获取");
                informed = true;
            }
            return true;
        } catch (IllegalStateException e) {
            note(verbose, "[Skin] " + PLUGIN_NAME + " API 未初始化: " + e.getMessage());
            return false;
        } catch (Throwable e) {
            // NoClassDefFoundError / LinkageError / 版本不兼容等，全部降级处理
            if (verbose && logger != null) {
                logger.warn("[Skin] 接入 " + PLUGIN_NAME + " 失败，回退到玩家档案与 Mojang 档案", e);
            }
            return false;
        }
    }

    private static void note(boolean verbose, String message) {
        if (!verbose || logger == null || informed) {
            return;
        }
        informed = true;
        logger.info(message);
    }

}
