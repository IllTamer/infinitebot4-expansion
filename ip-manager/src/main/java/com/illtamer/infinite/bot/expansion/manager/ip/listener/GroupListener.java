package com.illtamer.infinite.bot.expansion.manager.ip.listener;

import com.illtamer.infinite.bot.expansion.manager.ip.BindData;
import com.illtamer.infinite.bot.expansion.manager.ip.IPManager;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;

import java.util.Map;

public class GroupListener implements Listener {

    private final ExpansionConfig configFile;
    private final Language lang;
    private final Map<Long, BindData> bind;

    public GroupListener(IPManager instance) {
        configFile = instance.getConfigFile();
        lang = instance.getLanguage();
        bind = instance.getBind();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGroupMessage(GroupMessageEvent event) {
        String msg = event.getRawMessage().trim();
        long qq = event.getUserId();
        BindData data = bind.get(qq);
        if (data == null || !data.getCode().equalsIgnoreCase(msg)) {
            return;
        }

        synchronized (configFile) {
            configFile.getConfig().set(data.getUuid(), data.getIp());
            configFile.save();
        }
        bind.remove(qq, data);
        event.reply(PluginUtil.parseColor(lang.get("message", "success")));
    }

    @EventHandler
    public void onCommand(GroupMessageEvent event) {
        if (!StaticAPI.isAdmin(event.getUserId())) {
            return;
        }

    }

}
