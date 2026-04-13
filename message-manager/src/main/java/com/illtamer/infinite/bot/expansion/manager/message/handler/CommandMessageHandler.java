package com.illtamer.infinite.bot.expansion.manager.message.handler;

import com.illtamer.infinite.bot.expansion.manager.message.config.MessageConfig;
import com.illtamer.infinite.bot.expansion.manager.message.eneity.CapturingCommandSource;
import com.illtamer.infinite.bot.expansion.manager.message.eneity.SubmitSender;
import com.illtamer.infinite.bot.expansion.manager.message.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.manager.message.util.MessageUtil;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionSet;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
        OfflinePlayer offlinePlayer = MessageUtil.getPlayer(event);
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
                executeAndCapture(sender, cmd);
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

    /**
     * 在1.21版本中执行指令并捕获响应
     * @param command 要执行的指令
     * @return 捕获的响应内容列表
     */
    public static List<String> executeAndCapture(CommandSender sender, String command) {
        try {
            // 获取Minecraft服务器实例
            MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();

            // 创建捕获命令源
            CapturingCommandSource capturingSource = new CapturingCommandSource(sender);

            // 获取默认的命令源堆栈
            var defaultSource = nmsServer.createCommandSourceStack();

            // 创建新的命令源堆栈，使用我们的捕获源
            var commandSourceStack = defaultSource
                    .withSource(capturingSource)
                    .withPermission(PermissionSet.ALL_PERMISSIONS); // 设置权限级别为4（OP权限）

            // 执行指令
            nmsServer.getCommands().performPrefixedCommand(
                    commandSourceStack,
                    command.startsWith("/") ? command.substring(1) : command
            );

            // 返回捕获的消息
            return capturingSource.getCapturedMessages();
        } catch (Exception e) {
            e.printStackTrace();
            List<String> errorResult = new ArrayList<>();
            errorResult.add("执行指令时发生错误: " + e.getMessage());
            return errorResult;
        }
    }

}
