package com.illtamer.infinite.bot.expansion.basic.listener;

import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.notice.group.GroupMemberJoinEvent;
import com.illtamer.infinite.bot.api.event.request.GroupRequestEvent;
import com.illtamer.infinite.bot.api.exception.APIInvokeException;
import com.illtamer.infinite.bot.api.handler.OpenAPIHandling;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;

public class MemberMenageListener implements Listener {
    private final boolean accept;
    private final boolean autoRename;
    private final boolean changeAdmin;
    private final String defaultCard;
    private final List<String> msgs;
    private final long welcomeDelay;

    public MemberMenageListener(ExpansionConfig configFile) {
        this.accept = configFile.getConfig().getBoolean("member-manage.auto-accept");
        this.autoRename = configFile.getConfig().getBoolean("member-manage.auto-change-card");
        this.defaultCard = configFile.getConfig().getString("member-manage.default-card");
        this.changeAdmin = configFile.getConfig().getBoolean("member-manage.change-admin");
        this.msgs = configFile.getConfig().getStringList("member-manage.welcome");
        this.welcomeDelay = configFile.getConfig().getLong("member-manage.welcome-delay", 20);
    }

    @EventHandler
    public void onRequest(GroupRequestEvent event) {
        if (!accept) {
            return;
        }
        if (StaticAPI.inGroups(event.getGroupId())) {
            event.approve();
        }
    }

    @EventHandler
    public void onJoin(GroupMemberJoinEvent event) {
        if (StaticAPI.inGroups(event.getGroupId())) {
            Bukkit.getScheduler().runTaskLater(Bootstrap.getInstance(), () -> {
                event.sendGroupMessage(
                        MessageBuilder.json()
                                .at(event.getUserId())
                                .text(StringUtil.toString(msgs).replace("{0}", event.getUserId().toString()))
                                .build()
                );
            }, welcomeDelay);
        }
    }

    @EventHandler
    public void onRename(GroupMessageEvent event) {
        if (!autoRename || !StaticAPI.inGroups(event.getGroupId())) {
            return;
        }
        if (!changeAdmin && event.getSender().getRole().equals("admin")) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> {
            PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
            if (data == null || (data.getPreferUUID() == null)) {
                if (defaultCard == null || defaultCard.length() == 0) return;
                if (event.getSender().getNickname().equals(defaultCard)) return;
                OpenAPIHandling.setGroupMemberCard(defaultCard, event.getSender().getUserId(), event.getGroupId());
                return;
            }
            String name = Bukkit.getOfflinePlayer(UUID.fromString(data.getPreferUUID())).getName();
            if (!event.getSender().getNickname().equals(name)) {
                try {
                    OpenAPIHandling.setGroupMemberCard(name, event.getSender().getUserId(), event.getGroupId());
                } catch (APIInvokeException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
