package com.illtamer.infinite.bot.expansion.chat.listener;

import com.illtamer.infinite.bot.api.entity.Group;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.handler.OpenAPIHandling;
import com.illtamer.infinite.bot.expansion.chat.ChatManager;
import com.illtamer.infinite.bot.expansion.chat.Global;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.api.event.Priority;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Group2GameListener implements Listener {

    private final boolean enable;
    private final String prefix;
    private final boolean global; // type
    private final Map<String, Object> prefixMapper;

    public Group2GameListener(ExpansionConfig configFile) {
        ConfigurationSection section = configFile.getConfig().getConfigurationSection("group-to-game");
        if (section == null)
            section = configFile.getConfig().createSection("group-to-game");
        this.enable = section.getBoolean("enable", false);
        this.prefix = PluginUtil.parseColor(section.getString("prefix", "[]"));
        this.global = "global".equalsIgnoreCase(section.getString("type"));
        this.prefixMapper = ChatManager.getInstance().getPrefixMapper();
    }

    @EventHandler(priority = Priority.LOWEST)
    public void onGroup(GroupMessageEvent event) {
        if (!enable) return;
        if (!prefixMapper.containsKey(event.getGroupId().toString())) return;
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> {
            if (global) { // console
                Bukkit.getConsoleSender().sendMessage(format(event));
            } else { // private
                final Collection<? extends Player> players = Bukkit.getOnlinePlayers().stream()
                        .filter(player -> {
                            final Object close = Global.getCloseMap().get(player.getUniqueId().toString());
                            return close == null || !((boolean) close);
                        })
                        .collect(Collectors.toList());
                if (players.size() == 0) return;
                final String format = format(event);
                players.forEach(player -> player.sendMessage(format));
            }
        });
    }

    private String format(GroupMessageEvent event) {
        return prefix
                .replace("%group_id%", event.getGroupId().toString())
                .replace("%group_card%", getGroupName(event.getGroupId()))
                .replace("%sender_id%", event.getSender().getUserId().toString())
                .replace("%sender_card%", event.getSender().getCard())
                + event.getRawMessage();
    }

    private static String getGroupName(long groupId) {
        final Optional<Group> first = OpenAPIHandling.getCacheGroups().stream()
                .filter(group -> group.getGroupId() == groupId)
                .findFirst();
        if (first.isPresent())
            return first.get().getGroupName();
        else
            return "Unknown Group";
    }

}
