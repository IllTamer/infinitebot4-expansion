package com.illtamer.infinite.bot.expansion.manager.login.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.expansion.manager.login.LoginManager;
import com.illtamer.infinite.bot.expansion.manager.login.entity.IPLocation;
import com.illtamer.infinite.bot.expansion.manager.login.util.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 归属地查询
 * */
public class IPLocationService {

    private static final Gson GSON = new Gson();

    private final boolean enable;
    private final String urlTemplate;
    private final String provinceField;
    private final String cityField;
    private final String ispField;
    private final long cacheMillis;
    private final boolean skipLocal;
    private final String localText;
    private final String unknownText;

    private final ConcurrentHashMap<String, CachedLocation> cache = new ConcurrentHashMap<>();

    public IPLocationService(boolean enable,
                             String urlTemplate,
                             String provinceField,
                             String cityField,
                             String ispField,
                             long cacheSeconds,
                             boolean skipLocal,
                             String localText,
                             String unknownText) {
        this.enable = enable;
        this.urlTemplate = urlTemplate;
        this.provinceField = provinceField;
        this.cityField = cityField;
        this.ispField = ispField;
        this.cacheMillis = Math.max(0L, cacheSeconds) * 1000L;
        this.skipLocal = skipLocal;
        this.localText = localText == null || localText.isEmpty() ? "内网" : localText;
        this.unknownText = unknownText == null || unknownText.isEmpty() ? "未知" : unknownText;
    }

    /**
     * 查询指定 IP 的归属地，任何异常都会兜底为 unknownText
     * */
    public IPLocation query(String ip) {
        if (!enable || ip == null || ip.isEmpty()) {
            return IPLocation.of(unknownText);
        }
        if (skipLocal && Utils.isLocalIp(ip)) {
            return IPLocation.of(localText);
        }

        // 缓存命中
        CachedLocation cached = cache.get(ip);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expireAt > now) {
            return cached.location;
        }

        IPLocation location = doQuery(ip);
        if (cacheMillis > 0L) {
            cache.put(ip, new CachedLocation(location, now + cacheMillis));
        }
        return location;
    }

    private IPLocation doQuery(String ip) {
        String url = urlTemplate.replace("%ip%", ip);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return IPLocation.of(unknownText);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                JsonObject json = GSON.fromJson(builder.toString(), JsonObject.class);
                if (json == null) return IPLocation.of(unknownText);

                String province = readField(json, provinceField);
                String city = readField(json, cityField);
                String isp = readField(json, ispField);
                return new IPLocation(province, city, isp);
            }
        } catch (Exception e) {
            LoginManager instance = LoginManager.getInstance();
            if (instance != null) {
                instance.getLogger().warn("IP 归属地查询失败(" + ip + "): " + e.getMessage());
            }
            return IPLocation.of(unknownText);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignore) {}
            }
        }
    }

    private String readField(JsonObject root, String path) {
        if (path == null || path.isEmpty()) return unknownText;
        String[] parts = path.split("\\.");
        JsonElement current = root;
        for (String part : parts) {
            if (current == null || !current.isJsonObject()) return unknownText;
            current = current.getAsJsonObject().get(part);
        }
        if (current == null || current.isJsonNull()) return unknownText;
        String value = current.isJsonPrimitive() ? current.getAsString() : current.toString();
        return value == null || value.isEmpty() ? unknownText : value;
    }

    /**
     * 清理过期缓存条目
     * */
    public void cleanup() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().expireAt <= now);
    }

    private static final class CachedLocation {
        private final IPLocation location;
        private final long expireAt;

        CachedLocation(IPLocation location, long expireAt) {
            this.location = location;
            this.expireAt = expireAt;
        }
    }

}
