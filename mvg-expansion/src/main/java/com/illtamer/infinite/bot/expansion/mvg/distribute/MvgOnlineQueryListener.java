package com.illtamer.infinite.bot.expansion.mvg.distribute;

import com.illtamer.infinite.bot.expansion.mvg.MvgExpansion;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinData;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinResolver;
import com.illtamer.infinite.bot.expansion.mvg.util.MainThreadCaller;
import com.illtamer.infinite.bot.minecraft.api.distribute.AbstractDistributedListener;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedEventContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 跨服在线玩家采集
 * <p>
 * 每个子服返回自己的在线玩家列表与头像；主节点合并后再交给 mvg 绘制 playerlist。
 * 头像使用已缓存的皮肤，未命中时用内置默认皮肤兜底 ——
 * mvg 的 playerlist 要求每个玩家都必须带合法的内联 PNG 头像，缺一个整张图就会渲染失败。
 * <p>
 * 同一份采集逻辑也对外暴露为 {@link #queryLocal}，供主节点在跨服链路不可用时降级为单机查询。
 * */
public class MvgOnlineQueryListener extends AbstractDistributedListener<OnlinePlayersPayload> {

    public static final String IDENTIFIER = "onlinePlayers";

    public static final String PARAM_MAX = "max";
    public static final String PARAM_INCLUDE_SKIN = "includeSkin";

    private final MvgExpansion expansion;
    private final SkinResolver skinResolver;

    public MvgOnlineQueryListener(MvgExpansion expansion, SkinResolver skinResolver) {
        super(expansion, OnlinePlayersPayload.class);
        this.expansion = expansion;
        this.skinResolver = skinResolver;
    }

    @Override
    public OnlinePlayersPayload handle(DistributedEventContext context) {
        return queryLocal(context);
    }

    /**
     * 在本服采集在线玩家列表
     * <p>
     * 既是分布式广播的响应实现，也是主节点"跨服不可用时回退单机"的入口。
     * 与 {@code MvgPlayerQueryListener} 相同，任何异常都必须转成带 error 的回包而不是向外抛出，
     * 否则旧版 ib4 的广播接收端不会回包，主节点只能空等到超时。
     * */
    public OnlinePlayersPayload queryLocal(DistributedEventContext context) {
        // 广播参数经 gson 往返后数字会变成 Double，这里统一按 Number 处理，避免 ClassCastException
        final int max = toInt(context.getParam(PARAM_MAX));
        final boolean includeSkin = !Boolean.FALSE.equals(context.getParam(PARAM_INCLUDE_SKIN));
        final String serverName = expansion.getServerName();
        try {
            OnlinePlayersPayload payload = MainThreadCaller.call(
                    () -> collect(max, includeSkin, serverName));
            return payload == null ? OnlinePlayersPayload.of(serverName, new ArrayList<>()) : payload;
        } catch (Throwable e) {
            expansion.getLogger().warn("[Distribute] 采集在线玩家失败", e);
            OnlinePlayersPayload payload = OnlinePlayersPayload.of(serverName, new ArrayList<>());
            payload.setError(e.toString());
            return payload;
        }
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    private OnlinePlayersPayload collect(int max, boolean includeSkin, String serverName) {
        List<OnlinePlayerEntry> entries = new ArrayList<>();
        SkinData fallback = skinResolver.getDefault(null);
        boolean truncated = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (max > 0 && entries.size() >= max) {
                truncated = true;
                break;
            }
            OnlinePlayerEntry entry = new OnlinePlayerEntry();
            entry.setName(player.getName());
            entry.setDisplay(player.getName());
            entry.setUuid(player.getUniqueId().toString());
            entry.setPing(player.getPing());
            entry.setGameMode(player.getGameMode().name().toLowerCase(Locale.ROOT));
            entry.setServerName(serverName);
            if (includeSkin) {
                SkinData skin = skinResolver.getCachedOrDefault(player.getUniqueId(), null);
                entry.setTexture(skin.getDataUri());
            } else {
                entry.setTexture(fallback.getDataUri());
            }
            entries.add(entry);
        }
        OnlinePlayersPayload payload = OnlinePlayersPayload.of(serverName, entries);
        payload.setTruncated(truncated);
        return payload;
    }

    private static int toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

}
