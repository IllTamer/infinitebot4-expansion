package com.illtamer.infinite.bot.expansion.mvg.skin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 一次皮肤「来源解析」的结果：贴图地址 + 模型 + 指纹
 * <p>
 * 指纹用于判断缓存是否还有效：同一个玩家的皮肤被更换后，来源地址（texture hash）会变，
 * 指纹随之变化，缓存据此重新下载，而不是等 TTL 到期。
 * */
public final class TextureRef {

    private final String url;
    private final String model;
    private final String fingerprint;

    public TextureRef(String url, String model, String source) {
        this.url = url;
        this.model = SkinData.MODEL_SLIM.equalsIgnoreCase(model) ? SkinData.MODEL_SLIM : SkinData.MODEL_CLASSIC;
        this.fingerprint = fingerprint(source == null ? "" : source, url);
    }

    /**
     * 皮肤 PNG 地址（textures.minecraft.net/texture/xxx 或自定义 URL）
     * */
    public String getUrl() {
        return url;
    }

    public String getModel() {
        return model;
    }

    /**
     * 来源 + 地址的短摘要，仅用于比较，不参与渲染
     * */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * URL 的最后一段（Mojang 的 texture hash），供镜像模板的 {@code {hash}} 占位符使用
     * */
    public String hash() {
        int index = url.lastIndexOf('/');
        return index < 0 ? url : url.substring(index + 1);
    }

    private static String fingerprint(String source, String url) {
        String raw = source + "|" + url;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(Character.forDigit((value >> 4) & 0xF, 16));
                builder.append(Character.forDigit(value & 0xF, 16));
            }
            return builder.substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必备算法，理论上不会走到这里
            return Integer.toHexString(raw.hashCode());
        }
    }

    @Override
    public String toString() {
        return "TextureRef{model=" + model + ", url=" + url + ", fingerprint=" + fingerprint + "}";
    }

}
