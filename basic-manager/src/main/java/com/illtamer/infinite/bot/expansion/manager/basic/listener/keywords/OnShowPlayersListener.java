package com.illtamer.infinite.bot.expansion.manager.basic.listener.keywords;

import com.illtamer.infinite.bot.expansion.manager.basic.pojo.DataOnShowPlayers;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.distribute.AbstractDistributedListener;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedEventContext;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import com.illtamer.perpetua.sdk.entity.transfer.entity.Client;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class OnShowPlayersListener extends AbstractDistributedListener<DataOnShowPlayers> {

    private final Language language;
    private final String online;
    private final boolean showOp;

    public OnShowPlayersListener(ExpansionConfig configFile, Language language, IExpansion expansion) {
        super(expansion, DataOnShowPlayers.class);
        this.language = language;
        FileConfiguration config = configFile.getConfig();
        final ConfigurationSection section = config.getConfigurationSection("key-word");
        this.online = section.getString("online");
        this.showOp = config.getBoolean("online.show-op", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShowPlayers(GroupMessageEvent event) {
        if (!online.equals(event.getRawMessage()) || !StaticAPI.isMaster()) {
            return;
        }
        event.setCancelled(true);

        // 创建事件上下文
        DistributedEventContext context = new DistributedEventContext();

        // TODO 优化方法调用，提取到本体中
        getProcessor().tryProcessEvent(getIdentifier(), context, result -> {
            List<DataOnShowPlayers> dataList = result.getDataList();
            List<Client> failedClientList = result.getFailedClientList();

            int opTotal = 0, playerTotal = 0;
            Map<String, Set<String>> serverOpMap = new HashMap<>();
            Map<String, Set<String>> serverPlayerMap = new HashMap<>();
            for (DataOnShowPlayers data : dataList) {
                String clientName = StringUtil.isBlank(data.getClientName()) ? "未命名客户端" : data.getClientName();
                serverOpMap.computeIfAbsent(clientName, k -> new HashSet<>()).addAll(data.getOpList());
                serverPlayerMap.computeIfAbsent(clientName, k -> new HashSet<>()).addAll(data.getPlayerList());
                opTotal += data.getOpList().size();
                playerTotal += data.getPlayerList().size();
            }

            if (serverOpMap.isEmpty() && serverPlayerMap.isEmpty()) {
                event.reply(language.get("key-word", "no-player"));
                return;
            }

            StringBuilder opStr = new StringBuilder();
            if (opTotal > 0 && showOp) {
                opStr.append("\n管理员: ");
                serverOpMap.forEach((key, value) -> {
                    if (!value.isEmpty()) {
                        opStr.append("\n- ").append(key)
                                .append("\n  - ").append(String.join(", ", value));
                    }
                });
            }
            StringBuilder playerStr = new StringBuilder();
            if (playerTotal > 0) {
                playerStr.append("\n玩家: ");
                serverPlayerMap.forEach((key, value) -> {
                    if (!value.isEmpty()) {
                        playerStr.append("\n- ").append(key)
                                .append("\n  - ").append(String.join(", ", value));
                    }
                });
            }

            String totalAmount = "服务器当前总人数: " + (opTotal == 0 ? playerTotal : String.format("%d(op: %d人)", opTotal + playerTotal, opTotal));
            String msgBuilder = totalAmount + opStr + playerStr +
                    (failedClientList.isEmpty() ? "" : "\n访问超时的子服: " + failedClientList.stream().map(Client::getClientName).collect(Collectors.joining(",")));
            event.reply(msgBuilder);
        }, e -> {
            log.error("<服务器在线>分布式事件处理异常", e);
            event.reply(language.get("key-word", "error"));
        });
    }

    @Override
    public DataOnShowPlayers handle(DistributedEventContext context) {
        DataOnShowPlayers data = new DataOnShowPlayers();
        data.setClientName(StaticAPI.getClient().getClientName());
        Collection<? extends Player> players = BukkitBootstrap.getInstance().getServer().getOnlinePlayers();
        if (players.isEmpty()) {
            return data;
        }

        for (Player player : players) {
            if (player.isOp()) {
                data.getOpList().add(PluginUtil.clearColor(player.getDisplayName()));
            } else {
                data.getPlayerList().add(PluginUtil.clearColor(player.getDisplayName()));
            }
        }
        return data;
    }

    @Override
    public String getIdentifier() {
        return "showPlayers";
    }

}
