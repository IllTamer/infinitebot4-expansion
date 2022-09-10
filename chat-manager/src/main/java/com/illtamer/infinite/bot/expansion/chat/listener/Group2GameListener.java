package com.illtamer.infinite.bot.expansion.chat.listener;

import com.illtamer.infinite.bot.api.entity.Group;
import com.illtamer.infinite.bot.api.entity.TransferEntity;
import com.illtamer.infinite.bot.api.entity.transfer.*;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.handler.OpenAPIHandling;
import com.illtamer.infinite.bot.api.message.Message;
import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.chat.ChatManager;
import com.illtamer.infinite.bot.expansion.chat.Global;
import com.illtamer.infinite.bot.expansion.chat.filter.MessageFilter;
import com.illtamer.infinite.bot.expansion.chat.util.AtUtil;
import com.illtamer.infinite.bot.expansion.view.util.DispatchUtil;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class Group2GameListener implements Listener {

    private final boolean enable;
    private final String prefix;
    private final boolean global; // type
    private final int parseLevel;
    private final Map<String, Object> prefixMapper;
    private final boolean expandChat;
    @Nullable
    private final MessageFilter filter;
    private final Language language;

    public Group2GameListener(ExpansionConfig configFile, Language language) {
        ConfigurationSection section = configFile.getConfig().getConfigurationSection("group-to-game");
        if (section == null)
            section = configFile.getConfig().createSection("group-to-game");
        this.enable = section.getBoolean("enable", false);
        this.prefix = PluginUtil.parseColor(section.getString("prefix", "[]"));
        this.global = "global".equalsIgnoreCase(section.getString("type"));
        this.parseLevel = section.getInt("parse-level");
        this.prefixMapper = ChatManager.getInstance().getPrefixMapper();
        this.expandChat = configFile.getConfig().getBoolean("expand-chat");
        this.filter = MessageFilter.MAP.get(section.getString("filter.mode"));
        if (filter != null) {
            filter.init(section.getStringList("filter.key-set"));
        }
        this.language = language;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGroup(GroupMessageEvent event) {
        if (!enable) return;
        if (!prefixMapper.containsKey(event.getGroupId().toString())) return;
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> {
            if (global) { // console
                Format format = new Format(0, event);
                final String stringFormat = format.stringFormat();
                if (stringFormat == null) return;
                Bukkit.getConsoleSender().sendMessage(stringFormat);
            } else { // private
                final Collection<? extends Player> players = Bukkit.getOnlinePlayers().stream()
                        .filter(player -> {
                            final Object close = Global.getCloseMap().get(player.getUniqueId().toString());
                            return close == null || !((boolean) close);
                        })
                        .collect(Collectors.toList());
                if (players.size() == 0) return;
                Format format = new Format(parseLevel, event);
                if (format.isStringFormat()) {
                    final String stringFormat = format.stringFormat();
                    if (stringFormat == null) return;
                    players.forEach(player -> player.sendMessage(stringFormat));
                } else {
                    final BaseComponent[] components = format.componentFormat();
                    if (components == null) return;
                    players.forEach(player -> player.spigot().sendMessage(components));
                    if (format.at) {
                        final String message = PluginUtil.parseColor(language.get("group-to-game", "at", "message").replace("%sender_name%", format.sender));
                        if (format.atAll) {
                            final String title1 = PluginUtil.parseColor(language.get("group-to-game", "at", "all.title1"));
                            final String title2 = PluginUtil.parseColor(language.get("group-to-game", "at", "all.title2").replace("%sender_name%", format.sender));
                            players.forEach(player -> AtUtil.all(title1, title2, message, player));
                        } else {
                            final String title1 = PluginUtil.parseColor(language.get("group-to-game", "at", "one.title1"));
                            final String title2 = PluginUtil.parseColor(language.get("group-to-game", "at", "one.title2").replace("%sender_name%", format.sender));
                            format.atTargets.forEach(player ->  AtUtil.one(title1, title2, message, player));
                        }
                    }
                }
            }
        });
    }

    private String getGroupName(long groupId) {
        final Optional<Group> first = OpenAPIHandling.getCacheGroups().stream()
                .filter(group -> group.getGroupId() == groupId)
                .findFirst();
        if (first.isPresent())
            return first.get().getGroupName();
        else
            return language.get("group-to-game", "unknown");
    }

    private class Format {
        private final int parseLevel;
        private final Long senderId;
        private final String replacedPrefix;
        private final Message message;

        private boolean at = false;
        private boolean atAll = false;
        private String sender;
        private final List<Player> atTargets = new ArrayList<>();

        public Format(int parseLevel, GroupMessageEvent event) {
            this.parseLevel = parseLevel;
            this.senderId = event.getSender().getUserId();
            String senderName = event.getSender().getCard();
            senderName = senderName.trim().length() == 0 ? event.getSender().getNickname() : senderName;
            senderName = senderName.trim().length() == 0 ? String.valueOf(event.getSender().getUserId()) : senderName;
            this.replacedPrefix = PluginUtil.parseColor(prefix
                    .replace("%group_id%", event.getGroupId().toString())
                    .replace("%group_card%", getGroupName(event.getGroupId()))
                    .replace("%sender_id%", senderId.toString())
                    .replace("%sender_card%", event.getSender().getCard())
                    .replace("%sender_name%", senderName));
            this.message = event.getMessage();
        }

        @Nullable
        private String stringFormat() {
            final List<String> cleanMessage = filter == null ? message.getCleanMessage() : filter.doFilter(message.getCleanMessage());
            if (cleanMessage.size() == 0) return null;
            return replacedPrefix + StringUtil.toString(cleanMessage);
        }

        @Nullable
        private BaseComponent[] componentFormat() {
            final List<String> cleanMessage = message.getCleanMessage();
            // 过滤规则存在时 部分过滤规则拒绝纯特殊消息
            if (filter != null && !filter.isEmpty() && cleanMessage.size() == 0 && filter.rejectNoText()) return null;

            final List<TransferEntity> entities = message.getMessageChain().getEntities();
            if (entities.size() == 0) return null;
            if (!passAtLeastOne(entities)) return null;
            BaseComponent[] components = new BaseComponent[entities.size()+1];
            components[0] = new TextComponent(replacedPrefix);
            for (int i = 0; i < entities.size(); ++ i)
                components[i+1] = stepFormat(entities.get(i));
            return components;
        }

        private boolean isStringFormat() {
            return parseLevel == 0;
        }

        @SuppressWarnings("deprecation")
        private BaseComponent stepFormat(@NotNull TransferEntity entity) {
            Assert.notNull(entity, "TransferEntity can not be null");
            if (entity instanceof Text) {
                String text = ((Text) entity).getText();
                return new TextComponent(text.replace("\r\n", "\n"));
            } else if (entity instanceof At) {
                if (!at) { // init at args
                    at = true;
                    final PlayerData senderData = StaticAPI.getRepository().queryByUserId(senderId);
                    if (senderData == null || (senderData.getUuid() == null && senderData.getValidUUID() == null))
                        this.sender = senderId.toString();
                    else {
                        String uuid = senderData.getUuid() == null ? senderData.getValidUUID() : senderData.getUuid();
                        this.sender = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                    }
                }
                String qqStr = ((At) entity).getQq();
                if ("all".equalsIgnoreCase(qqStr)) {
                    atAll = true;
                    return new TextComponent(ChatColor.AQUA + "@全体玩家" + ChatColor.RESET);
                } else {
                    Long qq = Long.parseLong(qqStr);
                    final PlayerData data = StaticAPI.getRepository().queryByUserId(qq);
                    if (data == null || (data.getUuid() == null && data.getValidUUID() == null)) {
                        // TODO 获取群成员名片
                        return new TextComponent(ChatColor.YELLOW + "@" + qq + ChatColor.RESET);
                    } else {
                        String uuid = data.getUuid() == null ? data.getValidUUID() : data.getUuid();
                        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                        if (offlinePlayer.getPlayer() != null)
                            atTargets.add(offlinePlayer.getPlayer());
                        return new TextComponent(ChatColor.YELLOW + "@" + offlinePlayer.getName() + ChatColor.RESET);
                    }
                }
            } else if (entity instanceof Face) {
                return new TextComponent("§7[表情]" + ChatColor.RESET);
            } else if (entity instanceof Image) {
                if (!expandChat || !StaticAPI.hasExpansion("ViewManager", "IllTamer"))
                    return new TextComponent("§7[图片]" + ChatColor.RESET);
                Image image = (Image) entity;
                final TextComponent component = new TextComponent("§a[图片]" + ChatColor.RESET);
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(language.get("group-to-game", "click"))));
                final UUID uuid = DispatchUtil.executeImageWrapper(image.getUrl(), false);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "vm-map//" + uuid));
                return component;
            } else if (entity instanceof Redbag) {
                final Redbag redbag = (Redbag) entity;
                return new TextComponent("§7[红包](" + redbag.getTitle() + ')' + ChatColor.RESET);
            } else if (entity instanceof Record) {
                return new TextComponent("§7[语音]" + ChatColor.RESET);
            } else if (entity instanceof Reply) {
                return new TextComponent("§f[回复消息]\n" + ChatColor.RESET);
            } else if (entity instanceof Share) {
                Share share = (Share) entity;
                final TextComponent component = new TextComponent("§a[" + share.getTitle() + "链接]" + ChatColor.RESET);
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(language.get("group-to-game", "click"))));
                component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, share.getUrl()));
                return component;
            } else if (entity instanceof Video) {
                return new TextComponent("§7[视频]" + ChatColor.RESET);
            } else {
                return new TextComponent("§7[Unsupported]" + ChatColor.RESET);
            }
        }

        private boolean passAtLeastOne(List<TransferEntity> entities) {
            if (filter == null) return true;
            for (TransferEntity entity : entities) {
                if (entity instanceof Text && filter.result(((Text) entity).getText())) {
                    return true;
                }
            }
            return false;
        }

    }

}
