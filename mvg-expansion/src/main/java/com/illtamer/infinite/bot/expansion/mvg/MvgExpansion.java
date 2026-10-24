package com.illtamer.infinite.bot.expansion.mvg;

import com.illtamer.infinite.bot.expansion.mvg.client.MvgClient;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgResourceWarmer;
import com.illtamer.infinite.bot.expansion.mvg.config.MvgConfig;
import com.illtamer.infinite.bot.expansion.mvg.distribute.MvgOnlineQueryListener;
import com.illtamer.infinite.bot.expansion.mvg.distribute.MvgPlayerQueryListener;
import com.illtamer.infinite.bot.expansion.mvg.lang.ItemNameTable;
import com.illtamer.infinite.bot.expansion.mvg.listener.ImageSender;
import com.illtamer.infinite.bot.expansion.mvg.listener.MvgKeywordListener;
import com.illtamer.infinite.bot.expansion.mvg.listener.SkinRefreshListener;
import com.illtamer.infinite.bot.expansion.mvg.render.CustomDataSupport;
import com.illtamer.infinite.bot.expansion.mvg.render.ItemsAdderHook;
import com.illtamer.infinite.bot.expansion.mvg.render.MvgRenderService;
import com.illtamer.infinite.bot.expansion.mvg.render.TooltipBuilder;
import com.illtamer.infinite.bot.expansion.mvg.skin.MojangSkinSource;
import com.illtamer.infinite.bot.expansion.mvg.skin.ProfileSkinSource;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinCache;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinResolver;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinsRestorerHook;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinsRestorerSource;
import com.illtamer.infinite.bot.expansion.mvg.util.LegacyText;
import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

/**
 * mvg-expansion 附属主类
 * <p>
 * 把 QQ 消息中的查询请求转发给 minecraft-view-generator 渲染服务，
 * 并把渲染结果以图片形式回复给发送者。支持 Velocity 群组服（借助 ib4 的分布式事件）。
 * */
@Getter
public class MvgExpansion extends InfiniteExpansion {

    private static final int CONFIG_VERSION = 3;

    /**
     * 语言文件版本
     * <p>
     * 与 config.yml 分开维护：语言文件里的键会随功能增加而增加，
     * 版本号提升后 ib4 会把新键合并进服务器上已有的语言文件（保留用户改动）。
     * */
    private static final int LANGUAGE_VERSION = 2;

    private ExpansionConfig configFile;
    private Language language;
    private MvgConfig mvgConfig;

    private String serverName;

    private MvgClient client;
    private MvgResourceWarmer warmer;
    private SkinCache skinCache;
    private SkinResolver skinResolver;
    private MojangSkinSource mojangSkinSource;
    private ItemNameTable itemNameTable;
    private MvgRenderService renderService;
    private ImageSender imageSender;

    private MvgPlayerQueryListener playerQueryListener;
    private MvgOnlineQueryListener onlineQueryListener;
    private MvgKeywordListener keywordListener;

    private volatile boolean disabled;

    @Override
    public void onEnable() {
        disabled = false;
        configFile = new ExpansionConfig("config.yml", this, CONFIG_VERSION);
        language = Language.of("language", LANGUAGE_VERSION, this);
        mvgConfig = new MvgConfig(configFile.getConfig());
        serverName = resolveServerName();

        itemNameTable = ItemNameTable.load(getResource("item-names-zh_cn.json"));
        if (itemNameTable.isEmpty()) {
            getLogger().warn("未加载到内置中文名称表，物品名称将显示为英文（item-names-zh_cn.json）");
        }
        client = new MvgClient(mvgConfig.baseUrl, mvgConfig.apiKey, mvgConfig.connectTimeoutSeconds,
                mvgConfig.requestTimeoutSeconds, getLogger());
        warmer = createWarmer();

        long ttlMillis = TimeUnit.DAYS.toMillis(Math.max(0, mvgConfig.skinCacheDays));
        skinCache = new SkinCache(getDataFolder(), ttlMillis,
                getResource("default-skin-classic.png"), getResource("default-skin-slim.png"), getLogger());
        skinResolver = new SkinResolver(skinCache, mvgConfig.skinMaxDownloadBytes,
                mvgConfig.connectTimeoutSeconds, getLogger(),
                mvgConfig.skinTextureHosts, mvgConfig.skinVerifyOnJoin);
        SkinsRestorerHook.init(getLogger());
        // 解析链按优先级注册：SkinsRestorer（本地存储）→ 在线档案 → Mojang 按名查询
        skinResolver.addSource(new SkinsRestorerSource());
        skinResolver.addSource(new ProfileSkinSource(getLogger()));
        if (mvgConfig.skinMojangLookup) {
            mojangSkinSource = new MojangSkinSource(mvgConfig.skinMojangApiBase,
                    mvgConfig.skinSessionApiBase, mvgConfig.connectTimeoutSeconds, getLogger());
            skinResolver.addSource(mojangSkinSource);
        }
        getLogger().info("[Skin] 皮肤来源顺序: " + String.join(" → ", skinResolver.sourceNames()));
        ItemsAdderHook.init(getLogger(), mvgConfig.itemsAdder);
        CustomDataSupport.init(getLogger(), mvgConfig.itemsAdder, mvgConfig.customDataMode);
        getLogger().info("[Items] customData 下发范围: " + CustomDataSupport.mode()
                + (CustomDataSupport.isAlways()
                ? "（带正整数 custom_model_data 的物品都会下发，由 mvg 按「材料 + 数值」查索引）"
                : "（只下发 ItemsAdder API 判定为自定义物品的物品）"));
        if (!CustomDataSupport.isAlways()) {
            getLogger().warn("[Items] render.custom-data=itemsadder 会漏掉"
                    + "「复用 ItemsAdder 数值但不归它托管」的物品（钓鱼、MMO 武器等）："
                    + "这些物品在客户端有自定义贴图，图片里却会画成原版材质；改成 always 才能一并下发");
        }

        TooltipBuilder tooltipBuilder = new TooltipBuilder(itemNameTable, mvgConfig, getLogger());
        renderService = new MvgRenderService(client, warmer, mvgConfig, skinResolver, getLogger());
        imageSender = new ImageSender(mvgConfig, getDataFolder(), getLogger());

        playerQueryListener = new MvgPlayerQueryListener(this, skinResolver, tooltipBuilder);
        onlineQueryListener = new MvgOnlineQueryListener(this, skinResolver);

        keywordListener = new MvgKeywordListener(this, mvgConfig, language, renderService,
                playerQueryListener, onlineQueryListener, imageSender);
        EventExecutor.registerEvents(keywordListener, this);
        EventExecutor.registerBukkitEvent(new SkinRefreshListener(this, skinResolver), this);
        // /ib4 load 重载后内存缓存是空的：异步把在线玩家的磁盘缓存回填到内存，
        // 否则这些玩家会被画成内置默认皮肤，直到他们重新进服
        warmOnlineSkins();

        if (mvgConfig.warmupEnabled) {
            warmer.start();
        } else {
            warmer.bypass();
        }
        getLogger().info(String.format("mvg-expansion 已启用 | 渲染服务: %s | 子服名称: %s | 渲染版本: %s",
                mvgConfig.baseUrl, serverName, MvgClient.SUPPORTED_VERSION));
    }

    @Override
    public void onDisable() {
        disabled = true;
        if (warmer != null) {
            warmer.stop();
        }
        if (imageSender != null) {
            imageSender.clear();
        }
        if (skinResolver != null) {
            skinResolver.close();
        }
        if (mojangSkinSource != null) {
            mojangSkinSource.close();
            mojangSkinSource = null;
        }
        if (skinCache != null) {
            skinCache.clear();
        }
        if (client != null) {
            client.close();
        }
        playerQueryListener = null;
        onlineQueryListener = null;
        keywordListener = null;
        getLogger().info("mvg-expansion 已卸载");
    }

    @Override
    public String getExpansionName() {
        return "MvgExpansion";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

    /**
     * 供玩家提示使用的预热状态描述
     * */
    public String describeWarmupState() {
        if (warmer == null) {
            return "未开始";
        }
        return warmer.describeState();
    }

    public boolean isDisabled() {
        return disabled;
    }

    private MvgResourceWarmer createWarmer() {
        // mvg 的渲染接口只接受 1.21.11，因此即使服务器版本不同也必须按该版本预热，否则渲染必然 400
        String version = resolveMinecraftVersion();
        return new MvgResourceWarmer(client, version, mvgConfig.warmupPollIntervalSeconds,
                mvgConfig.warmupTimeoutMinutes, mvgConfig.warmupRetryDelaySeconds, getLogger());
    }

    private String resolveMinecraftVersion() {
        String configured = mvgConfig.minecraftVersion;
        String serverVersion = safeServerVersion();
        if (configured != null && !configured.trim().isEmpty()) {
            if (!MvgClient.SUPPORTED_VERSION.equals(configured.trim())) {
                getLogger().warn(String.format(
                        "配置的 Minecraft 版本(%s)不是 mvg 支持的版本，已改用 %s 预热与渲染",
                        configured.trim(), MvgClient.SUPPORTED_VERSION));
                return MvgClient.SUPPORTED_VERSION;
            }
            return configured.trim();
        }
        if (serverVersion != null && !serverVersion.isEmpty()
                && !MvgClient.SUPPORTED_VERSION.equals(serverVersion)) {
            getLogger().warn(String.format(
                    "当前服务器版本为 %s，而 mvg 仅支持 %s 渲染，将按 %s 准备资源；"
                            + "客户端侧的道具图标/贴图以 %s 的资源为准",
                    serverVersion, MvgClient.SUPPORTED_VERSION,
                    MvgClient.SUPPORTED_VERSION, MvgClient.SUPPORTED_VERSION));
        }
        return MvgClient.SUPPORTED_VERSION;
    }

    private String safeServerVersion() {
        try {
            return Bukkit.getMinecraftVersion();
        } catch (Throwable e) {
            getLogger().warn("无法读取服务器 Minecraft 版本: " + e.getMessage());
            return null;
        }
    }

    /**
     * 把在线玩家的磁盘皮肤缓存回填到内存
     * <p>
     * 只做内存/磁盘读取，不发起任何网络下载。附属被重载时内存缓存会被清空，
     * 而玩家通常不会为此重新进服，不预热就会一直渲染成默认皮肤。
     * */
    private void warmOnlineSkins() {
        BotScheduler.runTask(() -> {
            try {
                int warmed = 0;
                int total = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    total++;
                    if (skinResolver.warmCache(player.getUniqueId())) {
                        warmed++;
                    }
                }
                if (total > 0) {
                    getLogger().info(String.format("[Skin] 已从磁盘预热 %d/%d 名在线玩家的皮肤缓存", warmed, total));
                }
            } catch (Throwable e) {
                getLogger().warn("[Skin] 预热在线玩家皮肤失败", e);
            }
        });
    }

    /**
     * 子服显示名
     * <p>
     * 群组服中每个子服的 MOTD 通常就是子服名，因此未显式配置时取 MOTD（去掉颜色码）
     * */
    private String resolveServerName() {
        if (mvgConfig.serverName != null && !mvgConfig.serverName.trim().isEmpty()) {
            return mvgConfig.serverName.trim();
        }
        try {
            String motd = Bukkit.getServer().getMotd();
            if (motd != null) {
                String plain = LegacyText.strip(LegacyText.colorize(motd)).trim();
                if (!plain.isEmpty()) {
                    return plain;
                }
            }
        } catch (Throwable e) {
            getLogger().warn("无法读取服务器 MOTD，将使用默认子服名称: " + e.getMessage());
        }
        return "Server";
    }

}
