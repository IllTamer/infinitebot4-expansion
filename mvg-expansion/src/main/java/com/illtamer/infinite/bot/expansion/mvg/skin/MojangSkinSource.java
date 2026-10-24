package com.illtamer.infinite.bot.expansion.mvg.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mojang 档案来源（按玩家名查询）
 * <p>
 * 离线服的玩家档案里没有 textures，SkinsRestorer 又常常只装在代理端，
 * 这时唯一可靠的通用来源就是按玩家名向 Mojang 查档案：
 * <pre>
 * GET {mojang-api-base}/users/profiles/minecraft/{name}   -> id
 * GET {session-api-base}/session/minecraft/profile/{id}   -> properties[textures]
 * </pre>
 * 结果按 UUID 缓存一段时间，避免玩家反复进出服时反复请求；两个 base 都可配置，
 * 国内线路不通时可以直接指向自建/第三方镜像，或直接关掉这条来源。
 * */
public class MojangSkinSource implements SkinSource {

    public static final String NAME = "mojang";

    /**
     * 查询结果的缓存时间：皮肤本身由 {@link SkinResolver} 按指纹判断是否变化，
     * 这里只避免短时间内重复打两次 HTTP
     * */
    private static final long CACHE_MILLIS = Duration.ofHours(1).toMillis();

    private final String mojangApiBase;
    private final String sessionApiBase;
    private final Logger logger;
    private final HttpClient http;

    private final Map<UUID, CachedLookup> cache = new ConcurrentHashMap<>();

    public MojangSkinSource(String mojangApiBase, String sessionApiBase,
                            int connectTimeoutSeconds, Logger logger) {
        this.mojangApiBase = trimTrailingSlash(mojangApiBase);
        this.sessionApiBase = trimTrailingSlash(sessionApiBase);
        this.logger = logger;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SkinLookup lookup(UUID uuid, String playerName) {
        if (uuid == null || StringUtil.isBlank(playerName)) {
            return SkinLookup.miss("缺少玩家名");
        }
        CachedLookup cached = cache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.lookup;
        }
        SkinLookup lookup = query(playerName);
        cache.put(uuid, new CachedLookup(lookup, System.currentTimeMillis() + CACHE_MILLIS));
        return lookup;
    }

    /**
     * 释放连接资源（附属卸载时调用）
     * */
    public void close() {
        try {
            http.shutdownNow();
        } catch (Throwable e) {
            debug("[Skin] 关闭 Mojang 查询客户端异常", e);
        }
    }

    private SkinLookup query(String playerName) {
        if (mojangApiBase.isEmpty() || sessionApiBase.isEmpty()) {
            return SkinLookup.miss("未配置 Mojang 接口地址");
        }
        try {
            String encoded = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
            JsonObject profile = getJson(mojangApiBase + "/users/profiles/minecraft/" + encoded);
            if (profile == null) {
                return SkinLookup.miss("Mojang 无该名称");
            }
            String id = string(profile, "id");
            if (StringUtil.isBlank(id)) {
                return SkinLookup.miss("Mojang 未返回 UUID");
            }
            JsonObject session = getJson(sessionApiBase + "/session/minecraft/profile/" + id);
            if (session == null) {
                return SkinLookup.miss("会话档案查询失败");
            }
            String value = textureValue(session);
            if (StringUtil.isBlank(value)) {
                return SkinLookup.miss("会话档案无 textures");
            }
            TextureValueParser.Result texture = TextureValueParser.parse(value);
            if (texture == null) {
                return SkinLookup.miss("textures 解析失败");
            }
            return SkinLookup.hit(new TextureRef(texture.getUrl(), texture.getModel(), NAME));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SkinLookup.miss("查询被中断");
        } catch (IOException | RuntimeException e) {
            debug("[Skin] Mojang 档案查询失败: " + playerName, e);
            return SkinLookup.miss("Mojang 查询失败(" + e.getClass().getSimpleName() + ")");
        }
    }

    private JsonObject getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "InfiniteBot4-MvgExpansion")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        if (status == 204 || status == 404) {
            // Mojang 对不存在的玩家名返回 204/404
            return null;
        }
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status);
        }
        JsonElement element = JsonParser.parseString(response.body());
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    /**
     * 从会话档案里取 {@code textures} 属性的 base64 值
     * */
    private static String textureValue(JsonObject session) {
        JsonElement properties = session.get("properties");
        if (properties == null || !properties.isJsonArray()) {
            return null;
        }
        JsonArray array = properties.getAsJsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject property = element.getAsJsonObject();
            if (!"textures".equals(string(property, "name"))) {
                continue;
            }
            return string(property, "value");
        }
        return null;
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void debug(String message, Throwable error) {
        if (logger != null) {
            logger.debug(message, error);
        }
    }

    private static final class CachedLookup {

        private final SkinLookup lookup;
        private final long expiresAt;

        private CachedLookup(SkinLookup lookup, long expiresAt) {
            this.lookup = lookup;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

    }

}
