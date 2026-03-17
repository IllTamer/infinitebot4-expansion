package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.illtamer.infinite.bot.expansion.manager.basic.BasicManager;
import com.illtamer.infinite.bot.expansion.manager.basic.enetity.CapturingCommandSource;
import com.illtamer.infinite.bot.expansion.manager.basic.enetity.SubmitSender;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.util.Assert;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionSet;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftServer;

import java.util.ArrayList;
import java.util.List;

public class SubmitListener implements Listener {

    private final String prefix;
    private final int delayTick;
    private final String senderName;
    private final Language language;

    public SubmitListener(ExpansionConfig configFile, Language language) {
        final FileConfiguration config = configFile.getConfig();
        this.prefix = config.getString("submit.prefix");
        this.delayTick = config.getInt("submit.delay-tick", 5);
        this.senderName = config.getString("submit.sender-name", "InfiniteBot-BasicManager#SubmitSender");
        this.language = language;
        Assert.notNull(prefix, "'submit.prefix' can not be null !");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSubmit(MessageEvent event) {
        if (StaticAPI.isAdmin(event.getSender().getUserId())) {
            final String rawMessage = event.getRawMessage();
            if (rawMessage.startsWith(prefix) && rawMessage.length() >= prefix.length() +  2) {
                String command = rawMessage.substring(prefix.length() + 1);
                final String reply = language.get("submit", "reply");
                if (!reply.isEmpty()) {
                    event.reply(reply);
                }
                SubmitSender sender = new SubmitSender(BukkitBootstrap.getInstance().getServer(), event::reply, delayTick, senderName);
                // 主线程执行指令
                Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> executeAndCapture(sender, command));
                BasicManager.getInstance().getLogger().info(String.format(language.get("submit", "log"), event.getSender().getUserId(), command));
                event.setCancelled(true);
            }
        }
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