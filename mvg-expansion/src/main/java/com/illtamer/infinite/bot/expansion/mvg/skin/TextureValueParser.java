package com.illtamer.infinite.bot.expansion.mvg.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 皮肤 texture value 解析器
 * <p>
 * Minecraft 的皮肤属性（profile property {@code textures}）是一段 base64 编码的 JSON：
 * <pre>
 * {
 *   "textures": {
 *     "SKIN": {
 *       "url": "http://textures.minecraft.net/texture/&lt;hash&gt;",
 *       "metadata": { "model": "slim" }
 *     }
 *   }
 * }
 * </pre>
 * 这里自行解析出贴图地址与模型，而不依赖 SkinsRestorer 的工具方法 ——
 * 这样即使用户使用的是自定义 URL 皮肤（MineSkin 等），也能正确取到贴图。
 * */
public final class TextureValueParser {

    private TextureValueParser() {}

    /**
     * 解析结果
     * */
    public static class Result {

        private final String url;
        private final String model;

        Result(String url, String model) {
            this.url = url;
            this.model = model;
        }

        /**
         * 皮肤贴图地址
         * */
        public String getUrl() {
            return url;
        }

        /**
         * {@code classic} 或 {@code slim}
         * */
        public String getModel() {
            return model;
        }

        @Override
        public String toString() {
            return "TextureValue{model=" + model + ", url=" + url + "}";
        }

    }

    /**
     * 解析 base64 形式的 texture value
     *
     * @param value 皮肤属性值（base64）
     * @return 解析结果，失败或缺少 SKIN 时返回 {@code null}
     * */
    public static Result parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(value.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        try {
            JsonElement root = JsonParser.parseString(decoded);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonElement textures = root.getAsJsonObject().get("textures");
            if (textures == null || !textures.isJsonObject()) {
                return null;
            }
            JsonElement skin = textures.getAsJsonObject().get("SKIN");
            if (skin == null || !skin.isJsonObject()) {
                return null;
            }
            JsonObject skinObject = skin.getAsJsonObject();
            JsonElement urlElement = skinObject.get("url");
            if (urlElement == null || urlElement.isJsonNull() || urlElement.getAsString().isBlank()) {
                return null;
            }
            String url = urlElement.getAsString().trim();
            // 形如 http://textures.minecraft.net/texture/xxx 与 https 视为同址，统一升级为 https
            if (url.startsWith("http://")) {
                url = "https://" + url.substring("http://".length());
            }
            String model = SkinData.MODEL_CLASSIC;
            JsonElement metadata = skinObject.get("metadata");
            if (metadata != null && metadata.isJsonObject()) {
                JsonElement modelElement = metadata.getAsJsonObject().get("model");
                if (modelElement != null && !modelElement.isJsonNull()
                        && SkinData.MODEL_SLIM.equalsIgnoreCase(modelElement.getAsString())) {
                    model = SkinData.MODEL_SLIM;
                }
            }
            return new Result(url, model);
        } catch (RuntimeException e) {
            return null;
        }
    }

}
