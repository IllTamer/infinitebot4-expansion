package com.illtamer.infinite.bot.expansion.mvg.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * mvg 渲染结果（output=base64）
 * */
public class MvgRenderResult {

    private final String mimeType;
    private final String encoding;
    private final String data;
    private final int width;
    private final int height;

    private MvgRenderResult(String mimeType, String encoding, String data, int width, int height) {
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public static MvgRenderResult parse(JsonObject object) throws MvgApiException {
        String data = string(object, "data");
        if (data.isEmpty()) {
            throw new MvgApiException(200, "invalid_response", "mvg 渲染响应缺少 data 字段");
        }
        return new MvgRenderResult(
                string(object, "mimeType"),
                string(object, "encoding"),
                data,
                integer(object, "width"),
                integer(object, "height")
        );
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static int integer(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return 0;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getEncoding() {
        return encoding;
    }

    /**
     * 原始 base64 PNG（不含 data URI 前缀）
     * */
    public String getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * data URI 形式，便于直接落盘预览或二次处理
     * */
    public String toDataUri() {
        return "data:" + (mimeType.isEmpty() ? "image/png" : mimeType) + ";base64," + data;
    }

    @Override
    public String toString() {
        return "MvgRenderResult{" + width + "x" + height + ", " + data.length() + " chars}";
    }

}
