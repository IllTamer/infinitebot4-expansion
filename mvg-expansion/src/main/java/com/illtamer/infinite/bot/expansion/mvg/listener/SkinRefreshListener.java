package com.illtamer.infinite.bot.expansion.mvg.listener;

import com.illtamer.infinite.bot.expansion.mvg.MvgExpansion;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinResolver;
import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * 玩家进服时预热皮肤缓存
 * <p>
 * 皮肤解析涉及网络下载，全部丢到异步线程。进服即预热可以让后续的背包 / 在线列表渲染
 * 直接命中缓存 —— 跨服采集的回包等待上限只有 3 秒，那里绝不能发起下载。
 * */
public class SkinRefreshListener implements Listener {

    private final MvgExpansion expansion;
    private final SkinResolver skinResolver;

    public SkinRefreshListener(MvgExpansion expansion, SkinResolver skinResolver) {
        this.expansion = expansion;
        this.skinResolver = skinResolver;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!expansion.getMvgConfig().skinRefreshOnJoin) {
            return;
        }
        final String name = event.getPlayer().getName();
        final UUID uuid = event.getPlayer().getUniqueId();
        BotScheduler.runTask(() -> skinResolver.preload(uuid, name));
    }

}
