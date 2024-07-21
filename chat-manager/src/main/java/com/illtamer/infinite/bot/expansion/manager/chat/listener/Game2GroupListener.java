package com.illtamer.infinite.bot.expansion.manager.chat.listener;

import com.illtamer.infinite.bot.expansion.manager.chat.ChatManager;
import com.illtamer.infinite.bot.expansion.manager.chat.Global;
import com.illtamer.infinite.bot.expansion.manager.chat.event.PreGame2GroupMessageEvent;
import com.illtamer.infinite.bot.expansion.manager.chat.filter.MessageFilter;
import com.illtamer.infinite.bot.expansion.manager.chat.util.CheckUtil;
import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;
import com.illtamer.perpetua.sdk.message.Message;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
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
    private final boolean asyncRender;
    private final String prefix;
    private final Map<String, Object> prefixMapper;
    private final Set<Long> allTurnsGroupIds;
    private final Map<PreGame2GroupMessageEvent, AsyncPlayerChatEvent> eventMap = new HashMap<>();
    @Nullable
    private final MessageFilter filter;
    private final Language language;

    public Game2GroupListener(ExpansionConfig configFile, Language language) {
        ConfigurationSection section = configFile.getConfig().getConfigurationSection("game-to-group");
        if (section == null)
            section = configFile.getConfig().createSection("game-to-group");
        this.enable = section.getBoolean("enable", false);
        this.asyncRender = section.getBoolean("async-render", true);
        this.prefix = PluginUtil.parseColor(section.getString("prefix", "[]"));
        this.prefixMapper = ChatManager.getInstance().getPrefixMapper();
        this.allTurnsGroupIds = prefixMapper.entrySet().stream()
                .filter(entry -> ((String) entry.getValue()).length() == 0)
                .map(entry -> Long.parseLong(entry.getKey()))
                .collect(Collectors.toSet());
        this.filter = MessageFilter.MAP.get(section.getString("filter.mode"));
        if (filter != null) {
            filter.init(section.getStringList("filter.key-set"));
        }
        this.language = language;
    }

    // 较先触发(取消 InteractiveChat 消息)
    @EventHandler(priority = EventPriority.LOW)
    public void preChat(AsyncPlayerChatEvent event) {
        if (!enable || event.isCancelled()) return;
        final Player player = event.getPlayer();
        final String rawMessage = event.getMessage();
        if (filter != null && !filter.result(rawMessage)) {
            return;
        }

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
        final String finalCleanMessage = cleanMessage;
        Runnable runnable = () -> {
            PreGame2GroupMessageEvent messageEvent = new PreGame2GroupMessageEvent(asyncRender, Collections.unmodifiableSet(targetGroups), format(player), PluginUtil.clearColor(finalCleanMessage), player, event);
            eventMap.put(messageEvent, event);
            Bukkit.getPluginManager().callEvent(messageEvent);
        };
        if (asyncRender) {
            Bukkit.getScheduler().runTaskAsynchronously(((BukkitBootstrap) StaticAPI.getInstance()), runnable);
        } else {
            Bukkit.getScheduler().runTask(((BukkitBootstrap) StaticAPI.getInstance()), runnable);
        }
    }

    // 最后触发
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDispatch(PreGame2GroupMessageEvent messageEvent) {
        final AsyncPlayerChatEvent event = eventMap.remove(messageEvent);
        if (event == null) return;
        Message message = messageEvent.getMessage() == null ?
                MessageBuilder.json()
                        .text(messageEvent.getPrefix())
                        .text(messageEvent.getCleanMessage())
                        .build() :
                messageEvent.getMessage();
        BotScheduler.runTask(() -> messageEvent.getTargetGroups().forEach(messageConsumer(message)));
    }

    // 最先触发
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChange(AsyncPlayerChatEvent event) {
        final String message = event.getMessage();
        if (message.startsWith("#change")) {
            event.setCancelled(true);

            String[] splits = message.split(" ");
            if (splits.length != 2 || !CheckUtil.isNum(splits[1])) {
                event.getPlayer().sendMessage(language.get("game-to-group", "change-null"));
                return;
            }

            String uuid = event.getPlayer().getUniqueId().toString();
            String key = Global.gCloseKey(uuid, splits[1]);
            final Map<String, Object> closeMap = Global.getCloseMap();
            final Object close = closeMap.get(key);
            if (close == null || !((boolean) close)) { // close: false
                closeMap.put(key, true);
                event.getPlayer().sendMessage(language.get("game-to-group", "change-on").replace("%group_id%", splits[1]));
            } else { // close: true
                closeMap.put(key, false);
                event.getPlayer().sendMessage(language.get("game-to-group", "change-close").replace("%group_id%", splits[1]));
            }
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
