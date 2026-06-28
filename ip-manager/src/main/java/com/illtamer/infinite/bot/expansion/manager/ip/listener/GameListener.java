package com.illtamer.infinite.bot.expansion.manager.ip.listener;

import com.illtamer.infinite.bot.expansion.manager.ip.BindData;
import com.illtamer.infinite.bot.expansion.manager.ip.IPManager;
import com.illtamer.infinite.bot.expansion.manager.ip.Utils;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Map;

public class GameListener implements Listener {

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

        synchronized (configFile) {
            String oldIp = configFile.getConfig().getString(uuid);
            if (oldIp == null) {
                configFile.getConfig().set(uuid, ip);
                configFile.save();
                return;
            }
            if (ip.equals(oldIp)) {
                return;
            }
        }

        String code = Utils.getCode();
        bind.put(qq, new BindData(uuid, ip, code));

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, PluginUtil.parseColor(lang.get("message", "kick"))
                .replace("%qq%", Utils.encodeQQ(String.valueOf(qq))).replace("%code%", code));
    }

}
