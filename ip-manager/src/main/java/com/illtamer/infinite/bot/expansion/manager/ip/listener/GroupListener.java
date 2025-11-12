package com.illtamer.infinite.bot.expansion.manager.ip.listener;

import com.illtamer.infinite.bot.expansion.manager.ip.BindData;
import com.illtamer.infinite.bot.expansion.manager.ip.IPManager;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class GroupListener implements Listener {

    private final FileConfiguration config;
    private final Language lang;
    private final Map<Long, BindData> bind;

    public GroupListener(IPManager instance) {
        config = instance.getConfigFile().getConfig();
        lang = instance.getLanguage();
        bind = instance.getBind();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGroupMessage(GroupMessageEvent event) {
        String msg = event.getRawMessage();

        for (Map.Entry<Long, BindData> entry : bind.entrySet()) {
            if (!event.getUserId().equals(entry.getKey())) {
                continue;
            }
            BindData data = entry.getValue();
            if (!data.getCode().equals(msg)) {
                continue;
            }
            config.set(data.getUuid(), null);
            event.reply(PluginUtil.parseColor(lang.get("message", "success")));
            bind.remove(entry.getKey());
            return;
        }
    }

    @EventHandler
    public void onCommand(GroupMessageEvent event) {
        if (!StaticAPI.isAdmin(event.getUserId())) {
            return;
        }

    }

}
