package com.illtamer.infinite.bot.expansion.manager.basic.listener.keywords;

import com.illtamer.infinite.bot.expansion.manager.basic.enetity.SubmitSender;
import com.illtamer.infinite.bot.expansion.manager.basic.listener.SubmitListener;
import com.illtamer.infinite.bot.expansion.manager.basic.pojo.CmdResponse;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.distribute.AbstractDistributedListener;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedEventContext;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import com.illtamer.perpetua.sdk.entity.transfer.entity.Client;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class OnGlobalCmdListener extends AbstractDistributedListener<CmdResponse> {

    private final Language language;
    private final int delayTick;
    private final String senderName;
    private final String globalCmd;

    public OnGlobalCmdListener(ExpansionConfig configFile, Language language, IExpansion expansion) {
        super(expansion, CmdResponse.class);
        this.language = language;
        FileConfiguration config = configFile.getConfig();
        this.delayTick = config.getInt("submit.delay-tick", 5);
        this.senderName = "[G]" + config.getString("submit.sender-name", "InfiniteBot-BasicManager#SubmitSender");
        final ConfigurationSection section = config.getConfigurationSection("key-word");
        this.globalCmd = section.getString("global-cmd");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginOut(MessageEvent event) {
        if (!event.getRawMessage().startsWith(globalCmd) || !StaticAPI.isMaster()) {
            return;
        }
        if (!StaticAPI.isAdmin(event.getSender().getUserId())) {
            return;
        }
        event.setCancelled(true);

        String cmd = event.getRawMessage().replace(globalCmd, "").trim();
        if (StringUtil.isBlank(cmd)) {
            event.reply(language.get("keyword", "global-cmd-empty"));
            return;
        }

        // 创建事件上下文
        DistributedEventContext context = new DistributedEventContext();
        context.setParam("cmd", cmd);

        getProcessor().tryProcessEvent(getIdentifier(), context, result -> {
            // 汇总结果并回复
            if (!result.isAllFailed()) {
                // 所有客户端执行成功
                StringBuilder reply = new StringBuilder(language.get("key-word", "global-cmd-success"));
                for (CmdResponse data : result.getDataList()) {
                    if (StringUtil.isNotBlank(data.getResponse())) {
                        reply.append("\n").append(data.getClientName()).append(": ").append(data.getResponse());
                    }
                }
                for (Client failedClient : result.getFailedClientList()) {
                    reply.append("\n").append(failedClient.getClientName()).append(": 调用失败");
                }
                event.reply(reply.toString());
            } else {
                // 全部失败
                event.reply(language.get("key-word", "client-offline"));
            }
        }, e -> {
            log.error("全局指令分布式事件处理异常", e);
            event.reply(language.get("key-word", "error"));
        });
    }

    @Override
    public CmdResponse handle(DistributedEventContext context) {
        String cmd = context.getParam("cmd");
        log.info(language.get("keyword", "global-cmd-log"), cmd);
        CmdResponse response = new CmdResponse();
        response.setClientName(StaticAPI.getClient().getClientName());
        CompletableFuture<String> future = new CompletableFuture<>();

        SubmitSender sender = new SubmitSender(BukkitBootstrap.getInstance().getServer(), future::complete, delayTick, senderName);
        Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> SubmitListener.executeAndCapture(sender, cmd));
        try {
            response.setResponse(future.get());
        } catch (ExecutionException | InterruptedException e) {
            log.error(language.get("keyword", "global-cmd-error"), e);
        }
        return response;
    }

    @Override
    public String getIdentifier() {
        return "globalCmd";
    }

}
