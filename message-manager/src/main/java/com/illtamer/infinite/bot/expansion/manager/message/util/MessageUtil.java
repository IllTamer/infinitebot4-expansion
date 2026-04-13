package com.illtamer.infinite.bot.expansion.manager.message.util;

import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

@UtilityClass
public class MessageUtil {

    public static OfflinePlayer getPlayer(GroupMessageEvent event) {
        var data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getPreferUUID() == null) return null;
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(data.getPreferUUID()));
        } catch (Exception e) {
            return null;
        }
    }

}
