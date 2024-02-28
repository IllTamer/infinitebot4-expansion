package com.illtamer.infinite.bot.expansion.manager.chat.event;

import com.illtamer.perpetua.sdk.message.Message;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PreGame2GroupMessageEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * 目标群组
     * */
    private final Set<Long> targetGroups;
    /**
     * 转发消息前缀
     * */
    private final String prefix;
    /**
     * 去除颜色字符与前缀后的游戏消息
     * */
    private final String cleanMessage;
    private final Player player;
    private final AsyncPlayerChatEvent oriEvent;
    /**
     * 将要发送的 Message 对象
     * <p>
     * 可更改
     * */
    private Message message;

    public PreGame2GroupMessageEvent(boolean async, Set<Long> targetGroups, String prefix, String cleanMessage, Player player, AsyncPlayerChatEvent oriEvent) {
        super(async);
        this.targetGroups = targetGroups;
        this.prefix = prefix;
        this.cleanMessage = cleanMessage;
        this.player = player;
        this.oriEvent = oriEvent;
        this.message = MessageBuilder.json().text(prefix).text(cleanMessage).build();
    }

    public void cancelledOriEvent() {
        oriEvent.setCancelled(true);
    }

    public Set<Long> getTargetGroups() {
        return targetGroups;
    }

    public String getCleanMessage() {
        return cleanMessage;
    }

    public String getPrefix() {
        return prefix;
    }

    public Player getPlayer() {
        return player;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public String toString() {
        return "PreGame2GroupMessageEvent{" +
                "targetGroups=" + targetGroups +
                ", prefix='" + prefix + '\'' +
                ", cleanMessage='" + cleanMessage + '\'' +
                ", player=" + player +
                ", message=" + message +
                '}';
    }

}
