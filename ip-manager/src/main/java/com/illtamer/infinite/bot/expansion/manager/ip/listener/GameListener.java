package com.illtamer.infinite.bot.expansion.manager.ip.listener;

import com.illtamer.infinite.bot.expansion.manager.ip.BindData;
import com.illtamer.infinite.bot.expansion.manager.ip.IPManager;
import com.illtamer.infinite.bot.expansion.manager.ip.Utils;
import com.illtamer.infinite.bot.expansion.manager.ip.util.BotStatusUtil;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameListener implements Listener {

    /**
     * 机器人链接是否处于降级状态（边沿检测，避免日志刷屏）
     * */
    private static final AtomicBoolean DEGRADED = new AtomicBoolean(false);

    private final Map<Long, BindData> bind;
    private final Language lang;
    private final ExpansionConfig configFile;

    public GameListener(IPManager instance) {
        bind = instance.getBind();
        lang = instance.getLanguage();
        configFile = instance.getConfigFile();
    }

    @EventHandler
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        PlayerData playerData = StaticAPI.getRepository().queryByUUID(event.getUniqueId());
        if (playerData == null || playerData.getUserId() == null || playerData.getUserId() == 0L) {
            return;
        }

        long qq = playerData.getUserId();
        String uuid = event.getUniqueId().toString();
        String ip = event.getAddress().getHostAddress();

        String oldIp;
        synchronized (configFile) {
            oldIp = configFile.getConfig().getString(uuid);
            if (oldIp == null) {
                configFile.getConfig().set(uuid, ip);
                configFile.save();
                return;
            }
            if (ip.equals(oldIp)) {
                return;
            }
        }

        // 以下才是真正需要 QQ 验证的「IP 变更」路径
        // 机器人链接掉线/不可用时降级放行（否则玩家收不到验证码、也发不出验证码）
        if (!BotStatusUtil.isBotAvailable()) {
            if (DEGRADED.compareAndSet(false, true)) {
                IPManager.getInstance().getLogger().warn(
                        "机器人链接不可用(" + BotStatusUtil.describe() + ")，IPManager 登录拦截已降级，IP 变更玩家将直接放行");
            }
            IPManager.getInstance().getLogger().warn(
                    "机器人链接不可用，放行玩家 " + event.getName() + "(" + uuid + ")，IP " + oldIp + " -> " + ip + " 未校验");
            return;
        }
        if (DEGRADED.compareAndSet(true, false)) {
            IPManager.getInstance().getLogger().info("机器人链接已恢复，IPManager 登录拦截已恢复");
        }

        String code = Utils.getCode();
        bind.put(qq, new BindData(uuid, ip, code));

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, PluginUtil.parseColor(lang.get("message", "kick"))
                .replace("%qq%", Utils.encodeQQ(String.valueOf(qq))).replace("%code%", code));
    }

}
