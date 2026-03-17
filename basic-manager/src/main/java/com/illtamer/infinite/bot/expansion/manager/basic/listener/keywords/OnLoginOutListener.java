package com.illtamer.infinite.bot.expansion.manager.basic.listener.keywords;

import com.illtamer.infinite.bot.expansion.manager.basic.pojo.LoginOutData;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.distribute.AbstractDistributedListener;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedEventContext;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class OnLoginOutListener extends AbstractDistributedListener<LoginOutData> {

    private final Language language;
    private final String loginOut;

    public OnLoginOutListener(ExpansionConfig configFile, Language language, IExpansion expansion) {
        super(expansion, LoginOutData.class);
        this.language = language;
        FileConfiguration config = configFile.getConfig();
        final ConfigurationSection section = config.getConfigurationSection("key-word");
        this.loginOut = section.getString("login-out");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginOut(MessageEvent event) {
        if (!loginOut.equals(event.getRawMessage()) || !StaticAPI.isMaster()) {
            return;
        }
        event.setCancelled(true);

        // 创建事件上下文
        DistributedEventContext context = new DistributedEventContext();
        context.setParam("userId", String.valueOf(event.getUserId()));
        context.setParam("sender", event.getSender());

        getProcessor().tryProcessEvent(getIdentifier(), context, result -> {
            // 汇总结果并回复
            if (result.isAllSuccess()) {
                // 所有客户端执行成功
                StringBuilder reply = new StringBuilder(language.get("key-word", "kick-success"));
                for (LoginOutData data : result.getDataList()) {
                    if (!data.getKickedPlayers().isEmpty()) {
                        reply.append("\n").append(data.getClientName()).append(": ")
                                .append(String.join(", ", data.getKickedPlayers()));
                    }
                }
                event.reply(reply.toString());
            } else if (result.isPartialSuccess()) {
                // 部分成功
                event.reply(language.get("key-word", "kick-partial-success"));
            } else {
                // 全部失败
                event.reply(language.get("key-word", "offline"));
            }
        }, e -> {
            log.error("登录登出分布式事件处理异常", e);
            event.reply(language.get("key-word", "error"));
        });
    }

    @Override
    public LoginOutData handle(DistributedEventContext context) {
        Long userId = Long.parseLong(context.getParam("userId"));
        LoginOutData result = new LoginOutData(StaticAPI.getClient().getClientName());

        // TODO 优化为分发前获取数据
        PlayerData data = StaticAPI.getRepository().queryByUserId(userId);
        if (data == null || (data.getUuid() == null && data.getValidUUID() == null)) {
            return result;
        }

        List<Player> players = new ArrayList<>(2);
        if (data.getUuid() != null) {
            final Player player = Lambda.nullableInvoke(OfflinePlayer::getPlayer, Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid())));
            if (player != null) players.add(player);
        }
        if (data.getValidUUID() != null) {
            final Player player = Lambda.nullableInvoke(OfflinePlayer::getPlayer, Bukkit.getOfflinePlayer(UUID.fromString(data.getValidUUID())));
            if (player != null) players.add(player);
        }

        if (!players.isEmpty()) {
            Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> {
                players.forEach(player -> {
                    player.kickPlayer(PluginUtil.parseColor(language.get("key-word", "kick").replace("%qq%", String.valueOf(userId))));
                    result.addKickedPlayer(player.getName());
                });
            });
        }

        return result;
    }

    @Override
    public String getIdentifier() {
        return "loginOut";
    }

}
