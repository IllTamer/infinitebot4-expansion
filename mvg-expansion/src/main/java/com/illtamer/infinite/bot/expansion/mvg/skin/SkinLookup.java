package com.illtamer.infinite.bot.expansion.mvg.skin;

/**
 * 单个来源的解析结果
 * <p>
 * 命中时 {@link #getRef()} 非 {@code null}；未命中时 {@link #getReason()} 记录原因，
 * 供最终降级到默认皮肤时打出完整原因链，避免"全是默认皮肤但看不出为什么"。
 * */
public final class SkinLookup {

    private final TextureRef ref;
    private final String reason;

    private SkinLookup(TextureRef ref, String reason) {
        this.ref = ref;
        this.reason = reason;
    }

    public static SkinLookup hit(TextureRef ref) {
        return new SkinLookup(ref, null);
    }

    public static SkinLookup miss(String reason) {
        return new SkinLookup(null, reason == null || reason.isEmpty() ? "未命中" : reason);
    }

    public TextureRef getRef() {
        return ref;
    }

    public String getReason() {
        return reason;
    }

    public boolean isHit() {
        return ref != null;
    }

    @Override
    public String toString() {
        return isHit() ? ref.toString() : "miss(" + reason + ")";
    }

}
