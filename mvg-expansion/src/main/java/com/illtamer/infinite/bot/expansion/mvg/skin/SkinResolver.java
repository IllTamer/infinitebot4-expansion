package com.illtamer.infinite.bot.expansion.mvg.skin;

import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 皮肤解析编排
 * <p>
 * 解析顺序：内存/磁盘缓存 → 已注册的 {@link SkinSource}（SkinsRestorer / 在线档案 / Mojang 档案）
 * → 内置默认皮肤。只有 {@link #resolve(UUID, String, boolean)} / {@link #preload(UUID, String)}
 * 会发起网络请求，因此它们只能在异步线程调用；{@link #getCachedOrDefault(UUID, String)}
 * 与 {@link #getDefault(String)} 可在任意线程（含广播回包组装）直接使用。
 * <p>
 * 缓存命中不是无脑复用：磁盘/内存缓存会记录来源指纹，来源（texture hash）变了就重新下载，
 * 这样玩家换皮肤后不需要等 {@code skin.cache-days} 到期。
 * */
public class SkinResolver {

    /**
     * 皮肤贴图默认下载地址模板，可用配置覆盖；占位符：{@code {hash}} {@code {uuid}} {@code {name}}
     * */
    public static final List<String> DEFAULT_TEXTURE_HOSTS =
            Collections.singletonList("https://textures.minecraft.net/texture/{hash}");

    /**
     * 同一玩家重复告警的最小间隔，避免查询高峰刷屏
     * */
    private static final long WARN_INTERVAL_MILLIS = Duration.ofMinutes(10).toMillis();

    private final SkinCache cache;
    private final Logger logger;
    private final int maxDownloadBytes;
    private final HttpClient http;
    private final List<String> textureHosts;
    private final boolean verifyOnJoin;

    private final List<SkinSource> sources = new CopyOnWriteArrayList<>();
    private final Map<UUID, Long> lastWarnAt = new ConcurrentHashMap<>();

    public SkinResolver(SkinCache cache, int maxDownloadBytes, int connectTimeoutSeconds, Logger logger) {
        this(cache, maxDownloadBytes, connectTimeoutSeconds, logger, DEFAULT_TEXTURE_HOSTS, true);
    }

    public SkinResolver(SkinCache cache, int maxDownloadBytes, int connectTimeoutSeconds, Logger logger,
                        List<String> textureHosts, boolean verifyOnJoin) {
        this.cache = cache;
        this.maxDownloadBytes = Math.max(1024, maxDownloadBytes);
        this.logger = logger;
        this.textureHosts = textureHosts == null || textureHosts.isEmpty()
                ? DEFAULT_TEXTURE_HOSTS : new ArrayList<>(textureHosts);
        this.verifyOnJoin = verifyOnJoin;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 注册皮肤来源，注册顺序即解析优先级
     * */
    public void addSource(SkinSource source) {
        if (source != null) {
            sources.add(source);
        }
    }

    /**
     * 已注册的来源名（诊断用）
     * */
    public List<String> sourceNames() {
        List<String> names = new ArrayList<>();
        for (SkinSource source : sources) {
            names.add(source.name());
        }
        return names;
    }

    /**
     * 读取可用皮肤，未命中时返回默认皮肤
     * <p>
     * 这个方法会在分布式回包的<b>主线程</b>里被调用（背包 / 在线列表采集），
     * 因此优先只读内存。内存未命中时再回落到磁盘缓存 —— 典型场景是
     * 附属被 {@code /ib4 load} 重载后内存缓存为空，若这里直接返回默认皮肤，
     * 所有在线玩家都会被画成 Steve；单张皮肤只有几 KB，读取与 Base64 编码的代价可以忽略。
     * 只有内存与磁盘都没有时才使用内置默认皮肤。
     * */
    public SkinData getCachedOrDefault(UUID uuid, String modelHint) {
        SkinData cached = cache.getInMemory(uuid);
        if (cached != null) {
            return cached;
        }
        SkinData fromDisk = cache.get(uuid);
        if (fromDisk != null) {
            return fromDisk;
        }
        return cache.getDefault(modelHint);
    }

    /**
     * 把磁盘缓存回填到内存，不发起任何网络请求
     * <p>
     * 供玩家进服预热与附属重载后批量预热使用；下载与校验仍然只在
     * {@link #resolve(UUID, String, boolean)} 里发生。
     *
     * @return 是否命中缓存
     * */
    public boolean warmCache(UUID uuid) {
        return uuid != null && cache.get(uuid) != null;
    }

    /**
     * 释放下载皮肤用的连接资源（附属卸载时调用）
     * */
    public void close() {
        try {
            http.shutdownNow();
        } catch (Throwable e) {
            logger.debug("[Skin] 关闭 HTTP 客户端时出现异常", e);
        }
    }

    public SkinData getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public SkinData getDefault(String modelHint) {
        return cache.getDefault(modelHint);
    }

    public void clear() {
        cache.clear();
    }

    public int cachedCount() {
        return cache.size();
    }

    /**
     * 完整解析玩家皮肤（可能下载 PNG，必须在异步线程调用）
     *
     * @param uuid 玩家 UUID
     * @param playerName 玩家名，供 SkinsRestorer 与 Mojang 档案按名解析使用
     * @param force 是否忽略缓存强制重新解析
     * */
    public SkinData resolve(UUID uuid, String playerName, boolean force) {
        if (uuid == null) {
            return getDefault(SkinData.MODEL_CLASSIC);
        }
        SkinData cached = cache.get(uuid);
        Lookup result = lookup(uuid, playerName);
        if (cached != null && !force) {
            // 来源不可用（网络/插件问题）时不能因为"查不到"就丢掉已有缓存
            if (result.ref == null || !verifyOnJoin || cached.matches(result.ref.getFingerprint())) {
                return cached;
            }
            debug("[Skin] " + playerName + " 的皮肤来源已变化，重新下载", null);
        }
        if (result.ref != null) {
            SkinData downloaded = download(uuid, playerName, result.ref, result.source);
            if (downloaded != null) {
                cache.put(uuid, downloaded);
                return downloaded;
            }
        }
        if (cached != null) {
            return cached;
        }
        warnDefault(playerName, uuid, result.chain());
        return getDefault(result.ref == null ? SkinData.MODEL_CLASSIC : result.ref.getModel());
    }

    /**
     * 玩家进服时预热皮肤缓存（异步线程调用）
     * */
    public void preload(UUID uuid, String playerName) {
        try {
            SkinData data = resolve(uuid, playerName, false);
            if (logger != null && !data.isDefault()) {
                logger.debug(String.format("[Skin] 已缓存 %s 的皮肤(%s, %s)", playerName, data.getModel(), data.getSource()));
            }
        } catch (RuntimeException e) {
            logger.warn("[Skin] 预热皮肤失败: " + playerName, e);
        }
    }

    // ------------------------------------------------------------------ 来源解析

    /**
     * 依次询问各来源，返回第一个命中结果；未命中时保留每个来源的原因
     * */
    private Lookup lookup(UUID uuid, String playerName) {
        Map<String, String> reasons = new LinkedHashMap<>();
        for (SkinSource source : sources) {
            SkinLookup lookup;
            try {
                lookup = source.lookup(uuid, playerName);
            } catch (Throwable e) {
                // 来源实现理应自行兜底，这里再挡一层，保证解析链不中断
                debug("[Skin] 来源 " + source.name() + " 异常: " + uuid, e);
                reasons.put(source.name(), "异常(" + e.getClass().getSimpleName() + ")");
                continue;
            }
            if (lookup == null) {
                reasons.put(source.name(), "无结果");
                continue;
            }
            if (lookup.isHit()) {
                return new Lookup(lookup.getRef(), source.name(), reasons);
            }
            reasons.put(source.name(), lookup.getReason());
        }
        return new Lookup(null, null, reasons);
    }

    /**
     * 下载皮肤 PNG 并校验尺寸；按配置的地址模板依次尝试
     * */
    private SkinData download(UUID uuid, String playerName, TextureRef ref, String source) {
        List<String> candidates = candidates(ref, uuid, playerName);
        List<String> failures = new ArrayList<>();
        for (String url : candidates) {
            Download download = fetch(url, uuid, playerName);
            if (download == null) {
                failures.add(url);
                continue;
            }
            return new SkinData(download.dataUri, ref.getModel(), System.currentTimeMillis(),
                    source == null ? SkinData.SOURCE_DISK : source, ref.getFingerprint());
        }
        warn(playerName, uuid, "[Skin] 皮肤下载失败（已尝试 " + failures.size() + " 个地址）："
                + String.join(", ", failures));
        return null;
    }

    /**
     * 候选地址：来源给的原始地址在最前，其后是配置的镜像模板
     * */
    private List<String> candidates(TextureRef ref, UUID uuid, String playerName) {
        List<String> urls = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (ref.getUrl() != null && !ref.getUrl().isEmpty()) {
            urls.add(ref.getUrl());
            seen.add(ref.getUrl());
        }
        for (String template : textureHosts) {
            if (template == null || template.trim().isEmpty()) {
                continue;
            }
            String url = template.trim()
                    .replace("{hash}", ref.hash())
                    .replace("{uuid}", uuid == null ? "" : uuid.toString())
                    .replace("{name}", playerName == null ? "" : playerName);
            if (seen.add(url)) {
                urls.add(url);
            }
        }
        return urls;
    }

    /**
     * 下载并校验单个地址，失败返回 {@code null}
     * */
    private Download fetch(String url, UUID uuid, String playerName) {
        byte[] bytes;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "InfiniteBot4-MvgExpansion")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                debug(String.format("[Skin] 下载皮肤失败(%d): %s", response.statusCode(), url), null);
                return null;
            }
            bytes = response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException | RuntimeException e) {
            debug("[Skin] 下载皮肤异常: " + url, e);
            return null;
        }
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (bytes.length > maxDownloadBytes) {
            warn(playerName, uuid, String.format("[Skin] 皮肤文件超过大小上限(%d > %d): %s",
                    bytes.length, maxDownloadBytes, url));
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                debug("[Skin] 皮肤不是有效图片: " + url, null);
                return null;
            }
            if (!SkinCache.isValidSkinSize(image.getWidth(), image.getHeight())) {
                warn(playerName, uuid, String.format("[Skin] 皮肤尺寸不受 mvg 支持(%dx%d)，已忽略: %s",
                        image.getWidth(), image.getHeight(), url));
                return null;
            }
        } catch (IOException | RuntimeException e) {
            debug("[Skin] 校验皮肤图片失败: " + url, e);
            return null;
        }
        return new Download("data:image/png;base64," + Base64.getEncoder().encodeToString(bytes));
    }

    // ------------------------------------------------------------------ 日志

    /**
     * 降级到默认皮肤时打出完整原因链 —— 否则线上只能看到"全是默认皮肤"却无从判断原因
     * */
    private void warnDefault(String playerName, UUID uuid, String chain) {
        warn(playerName, uuid, String.format("[Skin] %s 未能解析到真实皮肤（%s）→ 使用内置默认皮肤",
                playerName == null || playerName.isEmpty() ? uuid : playerName,
                chain.isEmpty() ? "没有可用来源" : chain));
    }

    private void warn(String playerName, UUID uuid, String message) {
        if (logger == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = uuid == null ? null : lastWarnAt.get(uuid);
        if (previous != null && now - previous < WARN_INTERVAL_MILLIS) {
            return;
        }
        if (uuid != null) {
            lastWarnAt.put(uuid, now);
        }
        logger.warn(message);
    }

    private void debug(String message, Throwable error) {
        if (logger == null) {
            return;
        }
        if (error == null) {
            logger.debug(message);
        } else {
            logger.debug(message, error);
        }
    }

    /**
     * 一次来源解析的结果（命中时带来源名，未命中时带原因链）
     * */
    private static final class Lookup {

        private final TextureRef ref;
        private final String source;
        private final Map<String, String> reasons;

        private Lookup(TextureRef ref, String source, Map<String, String> reasons) {
            this.ref = ref;
            this.source = source;
            this.reasons = reasons;
        }

        private String chain() {
            if (reasons.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : reasons.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(entry.getKey()).append('=').append(entry.getValue());
            }
            return builder.toString();
        }

    }

    private static final class Download {

        private final String dataUri;

        private Download(String dataUri) {
            this.dataUri = dataUri;
        }

    }

}
