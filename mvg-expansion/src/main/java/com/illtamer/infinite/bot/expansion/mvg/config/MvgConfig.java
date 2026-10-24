package com.illtamer.infinite.bot.expansion.mvg.config;

import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * mvg-expansion 类型化配置
 * */
public class MvgConfig {

    public static final String IMAGE_MODE_BASE64 = "base64";
    public static final String IMAGE_MODE_FILE = "file";

    public static final String TOOLTIP_MODE_CHINESE = "chinese";
    public static final String TOOLTIP_MODE_VANILLA = "vanilla";

    // mvg
    public final String baseUrl;
    public final String apiKey;
    public final int connectTimeoutSeconds;
    public final int requestTimeoutSeconds;
    public final String minecraftVersion;

    /**
     * 子服名称，留空时取服务器 MOTD（群组服中 MOTD 通常就是子服名）
     * */
    public final String serverName;

    // warmup
    public final boolean warmupEnabled;
    public final int warmupPollIntervalSeconds;
    public final int warmupTimeoutMinutes;
    public final int warmupRetryDelaySeconds;

    // skin
    public final int skinCacheDays;
    public final boolean skinRefreshOnJoin;
    public final int skinMaxDownloadBytes;

    /**
     * 采集端拿不到皮肤模型时使用的兜底模型（classic / slim）
     * */
    public final String skinFallbackModel;

    /**
     * 进服解析时是否用来源指纹校验缓存：玩家换过皮肤就重新下载
     * */
    public final boolean skinVerifyOnJoin;

    /**
     * SkinsRestorer 与在线档案都拿不到时，是否按玩家名向 Mojang 查询皮肤（离线服主力来源）
     * */
    public final boolean skinMojangLookup;

    public final String skinMojangApiBase;
    public final String skinSessionApiBase;

    /**
     * 皮肤贴图下载地址模板（依次尝试），占位符：{@code {hash}} {@code {uuid}} {@code {name}}
     * */
    public final List<String> skinTextureHosts;

    // key-word
    public final String keywordInventory;
    public final String keywordMainHand;
    public final String keywordOffHand;
    public final String keywordEnderChest;
    public final String keywordOnline;

    // render
    public final String tooltipMode;
    public final boolean tooltipIcon;

    /**
     * 各视图的成品图放大倍率（mvg 的 {@code options.scale}），{@code 0}/{@code 1} 表示不放大
     * */
    public final double scaleInventory;
    public final double scaleEnderChest;
    public final double scaleTooltip;
    public final double scalePlayerList;

    /**
     * 是否把自定义物品的自定义模型数据作为 {@code customData} 传给 mvg
     * */
    public final boolean itemsAdder;

    /**
     * {@code customData} 的下发范围：{@code always}（只要有正整数 custom_model_data 就下发，默认）
     * 或 {@code itemsadder}（只下发 ItemsAdder 托管的物品，见 {@link com.illtamer.infinite.bot.expansion.mvg.render.CustomDataSupport}）
     * */
    public final String customDataMode;

    public final String inventoryTitle;
    public final String enderChestTitle;
    public final String playerListHeader;
    public final String playerListFooter;
    public final int playerListColumns;
    public final int playerListMaxPlayers;
    public final boolean onlineIncludeSkin;
    public final String imageUploadMode;
    public final int imageCacheSize;
    public final int cooldownSeconds;

    public MvgConfig(FileConfiguration config) {
        this.baseUrl = config.getString("mvg.base-url", "http://127.0.0.1:4666");
        this.apiKey = config.getString("mvg.api-key", "");
        this.connectTimeoutSeconds = config.getInt("mvg.connect-timeout-seconds", 5);
        this.requestTimeoutSeconds = config.getInt("mvg.request-timeout-seconds", 30);
        this.minecraftVersion = config.getString("mvg.minecraft-version", "");
        this.serverName = config.getString("mvg.server-name", "");

        this.warmupEnabled = config.getBoolean("warmup.enabled", true);
        this.warmupPollIntervalSeconds = config.getInt("warmup.poll-interval-seconds", 3);
        this.warmupTimeoutMinutes = config.getInt("warmup.timeout-minutes", 30);
        this.warmupRetryDelaySeconds = config.getInt("warmup.retry-delay-seconds", 60);

        this.skinCacheDays = config.getInt("skin.cache-days", 7);
        this.skinRefreshOnJoin = config.getBoolean("skin.refresh-on-join", true);
        this.skinMaxDownloadBytes = config.getInt("skin.max-download-bytes", 256 * 1024);
        this.skinFallbackModel = config.getString("skin.fallback-model", "classic");
        this.skinVerifyOnJoin = config.getBoolean("skin.verify-on-join", true);
        this.skinMojangLookup = config.getBoolean("skin.mojang-lookup", true);
        this.skinMojangApiBase = config.getString("skin.mojang-api-base", "https://api.mojang.com");
        this.skinSessionApiBase = config.getString("skin.session-api-base", "https://sessionserver.mojang.com");
        this.skinTextureHosts = textureHosts(config);

        this.keywordInventory = config.getString("key-word.inventory", "查看背包");
        this.keywordMainHand = config.getString("key-word.main-hand", "查看主手");
        this.keywordOffHand = config.getString("key-word.off-hand", "查看副手");
        this.keywordEnderChest = config.getString("key-word.ender-chest", "查看末影箱");
        this.keywordOnline = config.getString("key-word.online", "查看在线");

        this.tooltipMode = config.getString("render.tooltip-mode", TOOLTIP_MODE_CHINESE);
        this.tooltipIcon = config.getBoolean("render.tooltip-icon", false);
        this.scaleInventory = clampScale(config.getDouble("render.scale.inventory", 2D));
        this.scaleEnderChest = clampScale(config.getDouble("render.scale.enderchest", 2D));
        this.scaleTooltip = clampScale(config.getDouble("render.scale.tooltip", 2D));
        this.scalePlayerList = clampScale(config.getDouble("render.scale.playerlist", 2D));
        this.itemsAdder = config.getBoolean("render.itemsadder", true);
        this.customDataMode = normalizeCustomDataMode(config.getString("render.custom-data", "always"));
        this.inventoryTitle = config.getString("render.inventory-title", "%player% 的背包");
        this.enderChestTitle = config.getString("render.enderchest-title", "%player% 的末影箱");
        this.playerListHeader = config.getString("render.playerlist-header", "&a服务器在线 %total% 人");
        this.playerListFooter = config.getString("render.playerlist-footer", "");
        // 上限与 mvg 的校验保持一致，避免配置写错时收到 mvg 的英文报错
        this.playerListColumns = clamp(config.getInt("render.playerlist-columns", 0), 0, 16);
        this.playerListMaxPlayers = clamp(config.getInt("render.playerlist-max-players", 80), 1, 500);
        this.onlineIncludeSkin = config.getBoolean("render.online-include-skin", true);
        this.imageUploadMode = config.getString("render.image-upload-mode", IMAGE_MODE_BASE64);
        this.imageCacheSize = Math.max(1, config.getInt("render.image-cache-size", 32));
        this.cooldownSeconds = Math.max(0, config.getInt("render.cooldown-seconds", 3));
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * 收敛放大倍率：mvg 只接受 0–16，NaN 回落到默认的 2 倍
     * */
    private static double clampScale(double value) {
        if (Double.isNaN(value)) {
            return 2D;
        }
        if (value < 0) {
            return 0D;
        }
        return Math.min(16D, value);
    }

    /**
     * {@code render.custom-data} 只接受两个取值，写错时回落到默认的 {@code always}
     * */
    private static String normalizeCustomDataMode(String value) {
        String mode = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        // 只认显式的 itemsadder，其余（含写错/留空）都走默认的 always：
        // 严格模式会把"复用 ItemsAdder 数值但不由它托管"的物品误判掉，不适合当兜底
        return "itemsadder".equals(mode) ? "itemsadder" : "always";
    }

    /**
     * 皮肤下载地址模板；留空时使用 textures.minecraft.net
     * */
    private static List<String> textureHosts(FileConfiguration config) {
        List<String> hosts = config.getStringList("skin.texture-hosts");
        List<String> result = new ArrayList<>();
        for (String host : hosts) {
            if (host != null && !host.trim().isEmpty()) {
                result.add(host.trim());
            }
        }
        return result;
    }

    public boolean useChineseTooltip() {
        return !TOOLTIP_MODE_VANILLA.equalsIgnoreCase(tooltipMode);
    }

    public boolean useFileImage() {
        return IMAGE_MODE_FILE.equalsIgnoreCase(imageUploadMode);
    }

    /**
     * 关键词是否启用（配置为空即视为关闭该功能）
     * */
    public static boolean enabled(String keyword) {
        return StringUtil.isNotBlank(keyword);
    }

}
