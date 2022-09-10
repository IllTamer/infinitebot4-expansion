package com.illtamer.infinite.bot.expansion.message.listener;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.expansion.message.message.*;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageNode;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.repository.PlayerDataRepository;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public class MessageListener implements Listener {

    private final MessageTrigger trigger;
    private final PlayerDataRepository repository;

    public MessageListener() {
        this.trigger = new MessageTrigger(MessageLoader.MESSAGE_NODES);
        this.repository = StaticAPI.getRepository();
    }

    // all
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMessage(MessageEvent event) {
        final List<Pair<MessageNode, Object>> pairs = MessageLimit.check(trigger.select(event), event);
        if (pairs.size() == 0) return;
        event.setCancelled(true);
        final String uuid = Lambda.nullableInvoke(PlayerData::getPreferUUID, repository.queryByUserId(event.getUserId()));
        OfflinePlayer player = uuid != null ? Bukkit.getOfflinePlayer(UUID.fromString(uuid)) : null;
        PlaceholderHandler handler = new PlaceholderHandler(event, player);
        pairs.stream()
                .map(handler::replace)
                .forEach(entity -> ResponseHandler.handle(entity, event, player));
    }

}
