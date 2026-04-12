package com.illtamer.infinite.bot.expansion.manager.message.handler;

import com.illtamer.infinite.bot.expansion.manager.message.config.MessageConfig;
import com.illtamer.infinite.bot.expansion.manager.message.eneity.SubmitSender;
import com.illtamer.infinite.bot.expansion.manager.message.hook.Placeholder;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * 指令消息处理器
 * - console 身份：使用 SubmitSender 执行，可捕获并回复指令输出
 * - player 身份：以绑定玩家身份执行指令
 */
public class CommandMessageHandler implements MessageHandler {

    private final int delayTick;
    private final String senderName;
    private final MessageResponseSender responseSender;

    public CommandMessageHandler(int delayTick, String senderName, MessageResponseSender responseSender) {
        this.delayTick = delayTick;
        this.senderName = senderName;
        this.responseSender = responseSender;
    }

    @Override
    public void handle(GroupMessageEvent event, MessageConfig config) {
        OfflinePlayer offlinePlayer = getPlayer(event);
        List<String> commands = Placeholder.set(config.getCommandList(), offlinePlayer);

        if ("player".equalsIgnoreCase(config.getCommandIdentity())) {
            handlePlayerCommand(event, commands, offlinePlayer);
        } else {
            handleConsoleCommand(event, config, commands);
        }
    }

    private void handleConsoleCommand(GroupMessageEvent event, MessageConfig config, List<String> commands) {
        SubmitSender sender = new SubmitSender(
                Bukkit.getServer(),
                result -> responseSender.sendText(event, config, result),
                delayTick,
                senderName
        );
        Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> {
            for (String cmd : commands) {
                Bukkit.dispatchCommand(sender, cmd);
            }
        });
    }

    private void handlePlayerCommand(GroupMessageEvent event, List<String> commands, OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) return;
        Player player = offlinePlayer.getPlayer();
        if (player == null || !player.isOnline()) return;
        Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> {
            for (String cmd : commands) {
                player.performCommand(cmd);
            }
        });
    }

    private OfflinePlayer getPlayer(GroupMessageEvent event) {
        var data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getPreferUUID() == null) return null;
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(data.getPreferUUID()));
        } catch (Exception e) {
            return null;
        }
    }

}
