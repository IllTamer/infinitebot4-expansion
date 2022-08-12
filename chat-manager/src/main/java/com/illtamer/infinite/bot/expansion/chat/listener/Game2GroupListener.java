package com.illtamer.infinite.bot.expansion.chat.listener;

import com.illtamer.infinite.bot.api.handler.OpenAPIHandling;
import com.illtamer.infinite.bot.api.message.Message;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.expansion.chat.ChatManager;
import com.illtamer.infinite.bot.expansion.chat.Global;
import com.illtamer.infinite.bot.expansion.chat.event.PreGame2GroupMessageEvent;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Game2GroupListener implements Listener {
    private final boolean enable;
    private final String prefix;
    private final Map<String, Object> prefixMapper;
    private final Set<Long> allTurnsGroupIds;
    private final Map<PreGame2GroupMessageEvent, AsyncPlayerChatEvent> eventMap = new HashMap<>();

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

    // 较先触发(取消 InteractiveChat 消息)
    @EventHandler(priority = EventPriority.LOW)
    public void preChat(AsyncPlayerChatEvent event) {
        if (!enable || event.isCancelled()) return;
        final Player player = event.getPlayer();
        final String rawMessage = event.getMessage();

        Set<Long> targetGroups = new HashSet<>(allTurnsGroupIds);
        String cleanMessage = rawMessage;
        final int index;
        if (rawMessage.length() > 2 && (index = rawMessage.indexOf(' ')) != -1) {
            final String prefix = rawMessage.substring(0, index);
            final Set<Long> turnsGroupIds = prefixMapper.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(prefix))
                    .map(entry -> Long.parseLong(entry.getKey()))
                    .collect(Collectors.toSet());
            targetGroups.addAll(turnsGroupIds);
            cleanMessage = turnsGroupIds.size() == 0 ? rawMessage : rawMessage.substring(index+1);
        }
        PreGame2GroupMessageEvent messageEvent = new PreGame2GroupMessageEvent(Collections.unmodifiableSet(targetGroups), format(player), PluginUtil.clearColor(cleanMessage), player, event);
        eventMap.put(messageEvent, event);
        Bukkit.getPluginManager().callEvent(messageEvent);
    }

    // 最后触发
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDispatch(PreGame2GroupMessageEvent messageEvent) {
        final AsyncPlayerChatEvent event = eventMap.remove(messageEvent);
        if (event == null) return;
        Message message = messageEvent.getMessage() == null ?
                MessageBuilder.json().text(messageEvent.getPrefix()).text(messageEvent.getCleanMessage()).build() :
                messageEvent.getMessage();
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(),
                () -> messageEvent.getTargetGroups().forEach(Game2GroupListener.messageConsumer(message)));
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

    private String format(Player player) {
        return prefix
                .replace("%player_name%", player.getName());
    }

    public static Consumer<Long> messageConsumer(Message message) {
        return groupId -> {
            if (message == null) return;
            try {
                OpenAPIHandling.sendGroupMessage(message, groupId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

}
