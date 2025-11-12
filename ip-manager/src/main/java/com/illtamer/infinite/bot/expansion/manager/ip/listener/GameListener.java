package com.illtamer.infinite.bot.expansion.manager.ip.listener;

import com.illtamer.infinite.bot.expansion.manager.ip.BindData;
import com.illtamer.infinite.bot.expansion.manager.ip.IPManager;
import com.illtamer.infinite.bot.expansion.manager.ip.Utils;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Map;

public class GameListener implements Listener {

    private final Map<Long, BindData> bind;
    private final Language lang;
    private final ExpansionConfig configFile;
    private final FileConfiguration config;

    public GameListener(IPManager instance) {
        bind = instance.getBind();
        lang = instance.getLanguage();
        configFile = instance.getConfigFile();
        config = configFile.getConfig();
    }

    @EventHandler
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        PlayerData playerData = StaticAPI.getRepository().queryByUUID(event.getUniqueId());
        if (playerData == null || playerData.getUserId() == 0L) {
            return;
        }
        long qq = playerData.getUserId();
        String province = event.getAddress().getHostAddress();

        String uuid = event.getUniqueId().toString();
        if (!isSite(province, uuid)) {
            String code = Utils.getCode();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, PluginUtil.parseColor(lang.get("message", "kick"))
                    .replace("%qq%", Utils.encodeQQ(String.valueOf(qq))).replace("%code%", code));
            bind.put(qq, new BindData(uuid, code));
            return;
        }
        config.set(uuid, province);
        configFile.save();
    }

    private boolean isSite(String province, String uuid) {
        String old = config.getString(uuid);
        if (old == null) {
            return true;
        }
        return province.equals(old);
    }

}
