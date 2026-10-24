package com.illtamer.infinite.bot.expansion.mvg.skin;

/**
 * 已缓存并校验过的玩家皮肤材质
 * <p>
 * {@code dataUri} 形如 {@code data:image/png;base64,...}，是 mvg 渲染接口唯一接受的传图方式
 * （接口会拒绝一切 http(s) 链接，并要求尺寸为 64×64 或 legacy 64×32）。
 * */
public class SkinData {

    public static final String MODEL_CLASSIC = "classic";
    public static final String MODEL_SLIM = "slim";

    public static final String SOURCE_SKINSRESTORER = "skinsrestorer";
    public static final String SOURCE_PROFILE = "profile";
    public static final String SOURCE_DISK = "disk";
    public static final String SOURCE_DEFAULT = "default";

    private final String dataUri;
    private final String model;
    private final long fetchedAt;
    private final String source;

    /**
     * 来源指纹（{@link TextureRef#getFingerprint()}）
     * <p>
     * 缓存命中时用它和当前来源比对：玩家换过皮肤指纹就会变，据此重新下载，
     * 而不是傻等 {@code skin.cache-days} 到期。默认皮肤与历史缓存为空串，表示"无从校验"。
     * */
    private final String sourceKey;

    public SkinData(String dataUri, String model, long fetchedAt, String source) {
        this(dataUri, model, fetchedAt, source, "");
    }

    public SkinData(String dataUri, String model, long fetchedAt, String source, String sourceKey) {
        this.dataUri = dataUri;
        this.model = MODEL_SLIM.equalsIgnoreCase(model) ? MODEL_SLIM : MODEL_CLASSIC;
        this.fetchedAt = fetchedAt;
        this.source = source;
        this.sourceKey = sourceKey == null ? "" : sourceKey;
    }

    public String getDataUri() {
        return dataUri;
    }

    public String getModel() {
        return model;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }

    public String getSource() {
        return source;
    }

    /**
     * 来源指纹，空串表示该缓存没有记录来源（历史缓存 / 内置默认皮肤）
     * */
    public String getSourceKey() {
        return sourceKey;
    }

    /**
     * 缓存是否仍然对应当前来源
     * <p>
     * 没有记录来源指纹的历史缓存视为"无法校验"，返回 {@code false}：
     * 升级后每名玩家会按指纹重下一次（单张只有几 KB，且只在进服时发生），
     * 之后缓存即带上指纹，换肤能被立刻识别；下载失败时仍会沿用旧缓存。
     * */
    public boolean matches(String fingerprint) {
        return !sourceKey.isEmpty() && fingerprint != null && sourceKey.equals(fingerprint);
    }

    public boolean isDefault() {
        return SOURCE_DEFAULT.equals(source);
    }

    public boolean isExpired(long ttlMillis) {
        return ttlMillis > 0 && System.currentTimeMillis() - fetchedAt > ttlMillis;
    }

    @Override
    public String toString() {
        return "SkinData{" + model + ", " + source + ", " + dataUri.length() + " chars}";
    }

}
