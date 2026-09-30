package com.illtamer.infinite.bot.expansion.manager.basic.listener.keywords;

import com.illtamer.infinite.bot.expansion.manager.basic.enetity.SubmitSender;
import com.illtamer.infinite.bot.expansion.manager.basic.listener.SubmitListener;
import com.illtamer.infinite.bot.expansion.manager.basic.pojo.CmdResponse;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.distribute.AbstractDistributedListener;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedEventContext;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedResult;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.expansion.manager.basic.util.SchedulerCompat;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import com.illtamer.perpetua.sdk.entity.transfer.entity.Client;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class OnGlobalCmdListener extends AbstractDistributedListener<CmdResponse> {

    /**
     * 单个子服执行超时。必须小于核心分布式回调等待时间（3s），否则回调赶不上采集窗口
     * */
    private static final long HANDLE_TIMEOUT_MS = 2500L;

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
    public void onGlobalCmd(MessageEvent event) {
        if (StringUtil.isBlank(globalCmd) || !event.getRawMessage().startsWith(globalCmd) || !StaticAPI.isMaster()) {
            return;
        }
        if (!StaticAPI.isAdmin(event.getSender().getUserId())) {
            return;
        }
        event.setCancelled(true);

        String cmd = event.getRawMessage().substring(globalCmd.length()).trim();
        if (StringUtil.isBlank(cmd)) {
            event.reply(language.get("key-word", "global-cmd-empty"));
            return;
        }

        // 创建事件上下文
        DistributedEventContext context = new DistributedEventContext();
        context.setParam("cmd", cmd);

        getProcessor().tryProcessEvent(getIdentifier(), context, result ->
                replyResult(event, cmd, result), e -> {
            log.error("全局指令分布式事件处理异常", e);
            event.reply(language.get("key-word", "error"));
        });
    }

    /**
     * 将各子服执行结果合并为合并转发消息发出（每个子服单独一个节点）
     * */
    private void replyResult(MessageEvent event, String cmd, DistributedResult<CmdResponse> result) {
        if (result.isAllFailed()) {
            event.reply(language.get("key-word", "client-offline"));
            return;
        }
        final String fallbackText = buildPlainResult(cmd, result);
        try {
            final long uin = resolveNodeUin(event);
            final MessageBuilder nodes = MessageBuilder.json();
            nodes.customMessageNode(String.format(language.get("key-word", "global-cmd-title"), cmd), uin,
                    MessageBuilder.json().text(String.format(language.get("key-word", "global-cmd-summary"),
                            result.getSuccessCount(), result.getFailedCount())).build(), null);
            for (CmdResponse data : result.getDataList()) {
                nodes.customMessageNode(clientDisplayName(data.getClientName(), null), uin,
                        MessageBuilder.json().text(buildClientBody(data)).build(), null);
            }
            for (Client failedClient : result.getFailedClientList()) {
                nodes.customMessageNode(clientDisplayName(null, failedClient), uin,
                        MessageBuilder.json().text(language.get("key-word", "global-cmd-failed")).build(), null);
            }
            if (event instanceof GroupMessageEvent) {
                OpenAPIHandling.sendGroupForwardMessage(nodes.build(), ((GroupMessageEvent) event).getGroupId());
            } else {
                OpenAPIHandling.sendPrivateForwardMessage(nodes.build(), event.getUserId());
            }
        } catch (Exception e) {
            log.warn("全局指令合并转发发送失败，回退为纯文本回复", e);
            event.reply(fallbackText);
        }
    }

    private String buildClientBody(CmdResponse data) {
        String body = data.getResponse();
        if (StringUtil.isBlank(body)) {
            body = language.get("key-word", "global-cmd-no-output");
        }
        if (data.isTimeout()) {
            body = body + "\n" + language.get("key-word", "global-cmd-timeout");
        }
        return body;
    }

    private String buildPlainResult(String cmd, DistributedResult<CmdResponse> result) {
        StringBuilder reply = new StringBuilder(
                String.format(language.get("key-word", "global-cmd-title"), cmd));
        reply.append(' ').append(String.format(language.get("key-word", "global-cmd-summary"),
                result.getSuccessCount(), result.getFailedCount()));
        for (CmdResponse data : result.getDataList()) {
            reply.append('\n').append(clientDisplayName(data.getClientName(), null))
                    .append(": ").append(buildClientBody(data));
        }
        for (Client failedClient : result.getFailedClientList()) {
            reply.append('\n').append(clientDisplayName(null, failedClient))
                    .append(": ").append(language.get("key-word", "global-cmd-failed"));
        }
        return reply.toString();
    }

    private long resolveNodeUin(MessageEvent event) {
        Long uin = event.getSelfId();
        if (uin != null) {
            return uin;
        }
        try {
            return OpenAPIHandling.getLoginInfo().getUserId();
        } catch (Exception e) {
            log.warn("无法获取机器人 QQ 号，合并转发节点将使用默认标识", e);
            return 0L;
        }
    }

    private String clientDisplayName(String clientName, Client client) {
        if (StringUtil.isNotBlank(clientName)) {
            return clientName;
        }
        if (client != null) {
            if (StringUtil.isNotBlank(client.getClientName())) {
                return client.getClientName();
            }
            if (StringUtil.isNotBlank(client.getAppId())) {
                return client.getAppId();
            }
        }
        return "未知子服";
    }

    @Override
    public CmdResponse handle(DistributedEventContext context) {
        String cmd = context.getParam("cmd");
        log.info(language.get("key-word", "global-cmd-log"), cmd);

        CmdResponse response = new CmdResponse();
        response.setClientName(currentClientName());

        final List<String> outputs = Collections.synchronizedList(new ArrayList<>());
        final CompletableFuture<Void> executed = new CompletableFuture<>();
        // delayTick <= 0：每条 Bukkit 插件指令输出直接进入收集容器，由本方法统一等待后返回
        final SubmitSender sender = new SubmitSender(
                BukkitBootstrap.getInstance().getServer(), outputs::add, 0, senderName);
        SchedulerCompat.runTask(() -> {
            try {
                outputs.addAll(SubmitListener.executeAndCapture(sender, cmd));
            } catch (Throwable t) {
                log.error(language.get("key-word", "global-cmd-error"), t);
                outputs.add("执行指令时发生错误: " + t.getMessage());
            } finally {
                executed.complete(null);
            }
        });

        // 整体预算：等待执行 + 收集异步输出，必须在核心 3s 回调窗口内返回
        final long deadline = System.currentTimeMillis() + HANDLE_TIMEOUT_MS;
        try {
            // 等待主区域线程真正执行完指令
            executed.get(HANDLE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            // 留出少量时间收集异步插件指令输出
            final long settle = Math.min(Math.max(0L, delayTick) * 50L, Math.max(0L, deadline - System.currentTimeMillis()));
            if (settle > 0) {
                Thread.sleep(settle);
            }
            response.setResponse(String.join("\n", outputs));
        } catch (TimeoutException e) {
            response.setResponse(String.join("\n", outputs));
            response.setTimeout(true);
            log.warn("全局指令 '{}' 执行超时（已返回部分输出）", cmd);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setResponse(String.join("\n", outputs));
            response.setTimeout(true);
            log.error(language.get("key-word", "global-cmd-error"), e);
        } catch (Exception e) {
            log.error(language.get("key-word", "global-cmd-error"), e);
            response.setResponse("执行指令时发生错误: " + e.getMessage());
            response.setTimeout(true);
        }
        return response;
    }

    private String currentClientName() {
        Client client = StaticAPI.getClient();
        if (client == null) {
            return "未知子服";
        }
        if (StringUtil.isNotBlank(client.getClientName())) {
            return client.getClientName();
        }
        return StringUtil.isNotBlank(client.getAppId()) ? client.getAppId() : "未知子服";
    }

    @Override
    public String getIdentifier() {
        return "globalCmd";
    }

}
