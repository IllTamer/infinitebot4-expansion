package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.api.scheduler.MinecraftScheduler;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import com.illtamer.perpetua.sdk.event.notice.group.GroupMemberJoinEvent;
import com.illtamer.perpetua.sdk.event.request.GroupRequestEvent;
import com.illtamer.perpetua.sdk.exception.APIInvokeException;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
            MinecraftScheduler.runTaskLaterAsync(() -> {
                event.sendGroupMessage(
                        MessageBuilder.json()
                                .at(event.getUserId())
                                .text(StringUtil.toString(msgs).replace("{0}", event.getUserId().toString()))
                                .build()
                );
            }, welcomeDelay * 50L, TimeUnit.MILLISECONDS);
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
        MinecraftScheduler.runTaskAsync(() -> {
            PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
            if (data == null || (data.getPreferUUID() == null)) {
                if (defaultCard == null || defaultCard.isEmpty()) return;
                if (event.getSender().getCard().equals(defaultCard)) return;
                OpenAPIHandling.setGroupMemberCard(event.getGroupId(), event.getSender().getUserId(), defaultCard);
                return;
            }
            String name = Bukkit.getOfflinePlayer(UUID.fromString(data.getPreferUUID())).getName();
            if (!event.getSender().getCard().equals(name)) {
                try {
                    OpenAPIHandling.setGroupMemberCard(event.getGroupId(), event.getSender().getUserId(), name);
                } catch (APIInvokeException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
