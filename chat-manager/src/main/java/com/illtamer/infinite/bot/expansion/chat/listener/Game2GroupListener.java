package com.illtamer.infinite.bot.expansion.chat.listener;

import com.illtamer.infinite.bot.api.handler.OpenAPIHandling;
import com.illtamer.infinite.bot.expansion.chat.ChatManager;
import com.illtamer.infinite.bot.expansion.chat.Global;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Game2GroupListener implements Listener {

    private final boolean enable;
    private final String prefix;
    private final Map<String, Object> prefixMapper;
    private final Set<Long> allTurnsGroupIds;

    public Game2GroupListener(ExpansionConfig configFile) {
        ConfigurationSection section = configFile.getConfig().getConfigurationSection("game-to-group");
        if (section == null)
            section = configFile.getConfig().createSection("game-to-group");
        this.enable = section.getBoolean("enable", false);
        this.prefix = PluginUtil.parseColor(section.getString("prefix", "[]"));
        this.prefixMapper = ChatManager.getInstance().getPrefixMapper();
        this.allTurnsGroupIds = prefixMapper.entrySet().stream()
                .filter(entry -> ((String) entry.getValue()).length() == 0)
                .map(entry -> Long.parseLong(entry.getKey()))
                .collect(Collectors.toSet());
    }

    // 最后触发
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enable || event.isCancelled()) return;
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> {
            final String message = event.getMessage();
            final int index;
            if (allTurnsGroupIds.size() != 0) {
                final String format = format(event);
                allTurnsGroupIds.forEach(messageConsumer(format));
            }
            if (message.length() <= 2 || (index = message.indexOf(' ')) == -1) return;
            final String prefix = message.substring(0, index);
            final Set<Long> turnsGroupIds = prefixMapper.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(prefix))
                    .map(entry -> Long.parseLong(entry.getKey()))
                    .collect(Collectors.toSet());
            final String format = format(event);
            turnsGroupIds.forEach(messageConsumer(format));
        });
    }

    // 最先触发
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChange(AsyncPlayerChatEvent event) {
        final String message = event.getMessage();
        if (message.length() == 7 && "#change".equalsIgnoreCase(message)) {
            final String uuid = event.getPlayer().getUniqueId().toString();
            final Map<String, Object> closeMap = Global.getCloseMap();
            final Object close = closeMap.get(uuid);
            if (close == null || !((boolean) close)) { // close: false
                closeMap.put(uuid, true);
                event.getPlayer().sendMessage("切换消息接收状态为：屏蔽");
            } else { // close: true
                closeMap.put(uuid, false);
                event.getPlayer().sendMessage("切换消息接收状态为：接收");
            }
            event.setCancelled(true);
        }
    }

    private String format(AsyncPlayerChatEvent event) {
        return prefix
                .replace("%player_name%", event.getPlayer().getName())
                + event.getMessage();
    }

    private static Consumer<Long> messageConsumer(String format) {
        return groupId -> {
            try {
                OpenAPIHandling.sendGroupMessage(format, groupId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

}
