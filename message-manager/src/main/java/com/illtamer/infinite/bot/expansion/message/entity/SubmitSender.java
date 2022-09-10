package com.illtamer.infinite.bot.expansion.message.entity;

import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SubmitSender implements ConsoleCommandSender {
    private static final int delayTick = 5;
    private final List<String> cacheMessages = new ArrayList<>();
    private final Server server;
    private final MessageEvent event;
    private final boolean op;
    private final String senderName;

    public SubmitSender(Server server, MessageEvent event, boolean op, String senderName) {
        this.server = server;
        this.event = event;
        this.op = op;
        this.senderName = senderName;
    }

    @Override
    public void sendMessage(@NotNull String s) {
        doSendMessage(s);
    }

    @Override
    public void sendMessage(@NotNull String[] strings) {
        sendMessage(StringUtil.toString(Arrays.asList(strings)));
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {
        doSendMessage(s);
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String[] strings) {
        sendMessage(uuid, StringUtil.toString(Arrays.asList(strings)));
    }

    @Override
    public @NotNull Server getServer() {
        return this.server;
    }

    @Override
    public @NotNull String getName() {
        return senderName;
    }

    @Override
    public @NotNull Spigot spigot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConversing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void acceptConversationInput(@NotNull String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean beginConversation(@NotNull Conversation conversation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void abandonConversation(@NotNull Conversation conversation, @NotNull ConversationAbandonedEvent conversationAbandonedEvent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRawMessage(@NotNull String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRawMessage(@Nullable UUID uuid, @NotNull String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPermissionSet(@NotNull String s) {
        return Optional.of(this.server.getConsoleSender()).map(c -> c.isPermissionSet(s)).orElse(true);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission permission) {
        return Optional.of(this.server.getConsoleSender()).map(c -> c.isPermissionSet(permission)).orElse(true);
    }

    @Override
    public boolean hasPermission(@NotNull String s) {
        return true/*Optional.of(this.server.getConsoleSender()).map(c -> c.isPermissionSet(s)).orElse(true)*/;
    }

    @Override
    public boolean hasPermission(@NotNull Permission permission) {
        return true/*Optional.of(this.server.getConsoleSender()).map(c -> c.isPermissionSet(permission)).orElse(true)*/;
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String s, boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String s, boolean b, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment permissionAttachment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void recalculatePermissions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOp() {
        return op;
    }

    @Override
    public void setOp(boolean b) {
        throw new UnsupportedOperationException();
    }

    private void doSendMessage(String s) {
        if (cacheMessages.size() == 0) {
            cacheMessages.add(s);
            Bukkit.getScheduler().runTaskLaterAsynchronously(Bootstrap.getInstance(), () -> {
                List<String> messages = new ArrayList<>(cacheMessages);
                cacheMessages.clear();
                if (messages.size() == 1)
                    event.reply(PluginUtil.clearColor(messages.get(0)));
                else
                    event.reply(PluginUtil.clearColor(StringUtil.toString(messages)));
            }, delayTick);
        } else {
            cacheMessages.add(s);
        }
    }

}
