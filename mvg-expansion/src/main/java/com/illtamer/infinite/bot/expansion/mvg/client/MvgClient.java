package com.illtamer.infinite.bot.expansion.mvg.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * minecraft-view-generator（mvg）HTTP 接口客户端
 * <p>
 * 仅使用 JDK 内置 {@link HttpClient}，不引入额外依赖。所有方法均为阻塞调用，
 * 必须在异步线程中执行，禁止在主线程（服务器 tick 线程）调用。
 *
 * @see <a href="http://127.0.0.1:4666/docs">mvg Scalar API Reference</a>
 * */
public class MvgClient {

    /**
     * mvg 渲染接口硬编码支持的版本
     * <p>
     * {@code POST /api/v1/render} 对 version 做严格相等校验，非该值一律 400，
     * 因此预热与渲染都必须使用此版本；资源准备接口虽然接受任意版本，但其它版本无法渲染。
     * */
    public static final String SUPPORTED_VERSION = "1.21.11";

    private final HttpClient http;
    private final Gson gson = new Gson();
    private final Logger logger;

    private final String baseUrl;
    private final String apiKey;
    private final Duration requestTimeout;

    public MvgClient(String baseUrl, String apiKey, int connectTimeoutSeconds,
                     int requestTimeoutSeconds, Logger logger) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.baseUrl = normalized;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.requestTimeout = Duration.ofSeconds(Math.max(1, requestTimeoutSeconds));
        this.logger = logger;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 存活检查：服务是否返回版本列表
     * <p>
     * 当前接口没有独立的健康检查路由（原 {@code /healthz} 已删除）：容器探针与客户端都读
     * {@code GET /api/v1/versions}，它不需要 API key、恒返回 200 与版本列表，
     * 只要 HTTP 栈与资源管理器可用就算存活。渲染后端是否就绪由渲染请求本身报告
     * （{@code 503 renderer_unavailable}），资源准备进度由
     * {@link #resourceState(String)} 体现。
     * */
    public boolean isHealthy() throws MvgApiException {
        JsonObject object = getJson("/api/v1/versions");
        JsonElement versions = object.get("versions");
        return versions != null && versions.isJsonArray();
    }

    /**
     * 请求准备指定版本的资源，返回当前状态
     * <p>
     * mvg 侧为异步准备：首次调用返回 202，已就绪时返回 200，需要配合
     * {@link #resourceState(String)} 轮询直到 status=ready
     * */
    public MvgResourceState prepareResource(String version) throws MvgApiException {
        return MvgResourceState.parse(send("PUT", "/api/v1/resources/" + version, null));
    }

    /**
     * 查询指定版本的资源准备状态
     * */
    public MvgResourceState resourceState(String version) throws MvgApiException {
        return MvgResourceState.parse(getJson("/api/v1/resources/" + version));
    }

    /**
     * 调用统一渲染接口，要求返回 base64 图片
     * */
    public MvgRenderResult render(JsonObject payload) throws MvgApiException {
        return MvgRenderResult.parse(send("POST", "/api/v1/render", payload));
    }

    /**
     * 释放底层连接资源（附属卸载时调用）
     * <p>
     * 使用 {@code shutdownNow()} 而不是 {@code close()}：后者会等待所有在途请求结束，
     * 而本方法是在服务器主线程（onDisable）调用的，等待最长 request-timeout 会卡住服务器。
     * */
    public void close() {
        try {
            http.shutdownNow();
        } catch (Throwable e) {
            logger.debug("[mvg] 关闭 HTTP 客户端时出现异常", e);
        }
    }

    private JsonObject getJson(String path) throws MvgApiException {
        return send("GET", path, null);
    }

    private JsonObject send(String method, String path, JsonObject body) throws MvgApiException {
        if (baseUrl.isEmpty()) {
            throw MvgApiException.network("未配置 mvg 服务地址", null);
        }
        URI uri;
        try {
            uri = URI.create(baseUrl + path);
        } catch (RuntimeException e) {
            throw MvgApiException.network("非法的 mvg 服务地址: " + baseUrl + path, e);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json");
        if (!apiKey.isEmpty()) {
            builder.header("X-API-Key", apiKey);
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json; charset=utf-8")
                    .method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        }
        HttpResponse<String> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw MvgApiException.network("mvg 请求被中断: " + method + " " + path, e);
        } catch (IOException e) {
            throw MvgApiException.network("无法连接 mvg 服务(" + baseUrl + "): " + e.getMessage(), e);
        }
        int status = response.statusCode();
        String text = response.body();
        if (status < 200 || status >= 300) {
            throw toApiException(status, text, method, path);
        }
        JsonObject object = parseObject(text, status, method, path);
        if (object == null) {
            throw new MvgApiException(status, "invalid_response", "mvg 返回了非 JSON 对象: " + brief(text));
        }
        return object;
    }

    private MvgApiException toApiException(int status, String text, String method, String path) {
        String code = "";
        String message = brief(text);
        JsonObject object = parseObject(text, status, method, path);
        if (object != null) {
            JsonElement error = object.get("error");
            if (error != null && error.isJsonObject()) {
                JsonObject errorObject = error.getAsJsonObject();
                JsonElement codeElement = errorObject.get("code");
                JsonElement messageElement = errorObject.get("message");
                if (codeElement != null && !codeElement.isJsonNull()) {
                    code = codeElement.getAsString();
                }
                if (messageElement != null && !messageElement.isJsonNull()) {
                    message = messageElement.getAsString();
                }
            }
        }
        logger.warn(String.format("[mvg] %s %s 返回 %d: %s(%s)", method, path, status, code, message));
        return new MvgApiException(status, code, message);
    }

    private JsonObject parseObject(String text, int status, String method, String path) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(text);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            return null;
        } catch (RuntimeException e) {
            logger.debug(String.format("[mvg] %s %s 返回体解析失败(%d)", method, path, status), e);
            return null;
        }
    }

    private static String brief(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }

    /**
     * 构造渲染请求报文
     * */
    public JsonObject buildRenderPayload(String type, JsonObject data) {
        return buildRenderPayload(type, data, 0D);
    }

    /**
     * 构造渲染请求报文，并按需写入成品图放大倍率
     *
     * @param scale mvg 的 {@code options.scale}，{@code <= 0} 表示不放大（不下发该字段）
     * */
    public JsonObject buildRenderPayload(String type, JsonObject data, double scale) {
        JsonObject payload = new JsonObject();
        payload.addProperty("version", SUPPORTED_VERSION);
        payload.addProperty("type", type);
        payload.addProperty("output", "base64");
        payload.add("data", data);
        if (scale > 0) {
            JsonObject options = new JsonObject();
            options.addProperty("scale", scale);
            payload.add("options", options);
        }
        return payload;
    }

    /**
     * 读取 {@code GET /api/v1/resources/sources} 的原始响应，供诊断与沙箱断言使用
     * */
    public JsonObject sources() throws MvgApiException {
        return getJson("/api/v1/resources/sources");
    }

    /**
     * 重建 ItemsAdder 自定义物品索引（会丢弃已缓存的模型与贴图），返回新的索引状态
     * <p>
     * 用户手工增删包或改动 {@code items_ids_cache.yml} 后调用即可生效，不需要重启 mvg。
     * */
    public JsonObject reloadCustom() throws MvgApiException {
        return send("POST", "/api/v1/resources/custom/reload", null);
    }

    /**
     * 读取 {@code GET /api/v1/versions} 的原始响应，供诊断使用
     * */
    public JsonObject versions() throws MvgApiException {
        return getJson("/api/v1/versions");
    }

    Gson gson() {
        return gson;
    }

}
