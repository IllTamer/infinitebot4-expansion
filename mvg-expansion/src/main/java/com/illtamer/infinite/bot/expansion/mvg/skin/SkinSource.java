package com.illtamer.infinite.bot.expansion.mvg.skin;

import java.util.UUID;

/**
 * 皮肤来源提供者
 * <p>
 * 只负责「这个玩家现在应该用哪个贴图」，不负责下载。抽成接口是为了：
 * 1. 解析链可以按配置自由组合（SkinsRestorer / 在线档案 / Mojang 档案）；
 * 2. 离线沙箱可以注入 stub 来源，驱动"指纹变化 → 重新下载"等分支，
 *    不依赖 SkinsRestorer 插件、也不发起真实网络请求。
 * <p>
 * 实现必须自行捕获所有异常并返回 {@link SkinLookup#miss(String)}：
 * 任一来源失败都不能打断整条解析链。
 * */
public interface SkinSource {

    /**
     * 来源标识，用于日志与缓存指纹
     * */
    String name();

    /**
     * 解析玩家当前皮肤的贴图地址与模型
     * <p>
     * 可能发起网络请求（如 Mojang 档案查询），因此只能在异步线程调用。
     *
     * @param uuid 玩家 UUID
     * @param playerName 玩家名，部分来源需要（Mojang 按名查询、SkinsRestorer 兜底）
     * @return 命中或未命中（含原因），永不返回 {@code null}
     * */
    SkinLookup lookup(UUID uuid, String playerName);

}
