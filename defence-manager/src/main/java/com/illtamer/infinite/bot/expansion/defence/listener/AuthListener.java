package com.illtamer.infinite.bot.expansion.defence.listener;

import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.event.notice.group.GroupMemberQuitEvent;
import com.illtamer.infinite.bot.expansion.defence.DefenceManager;
import com.illtamer.infinite.bot.expansion.defence.entity.AuthData;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.api.event.Priority;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.repository.PlayerDataRepository;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;

public class AuthListener implements Listener {

    private final boolean removeOnLeave;
    private final String success;
    private final String failure;

    public AuthListener(ExpansionConfig configFile) {
        final FileConfiguration config = configFile.getConfig();
        this.removeOnLeave = config.getBoolean("remove-on-leave");
        this.success = config.getString("success");
        this.failure = config.getString("failure");
    }

    @EventHandler(priority = Priority.HIGHEST)
    public void onAuth(MessageEvent event) {
        String msg = event.getRawMessage();
        if (!msg.startsWith("验证 ")) {
            if (StaticAPI.isAdmin(event.getSender().getUserId()) && msg.startsWith("清空缓存 ")) {
                int value = Integer.parseInt(msg.split(" ")[1]);
                LoginListener.clearCommon(value);
                event.reply("Done-");
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        if (msg.length() < 4) {
            event.reply("请核实格式 '验证 <code>'");
            return;
        }
        final PlayerDataRepository repository = StaticAPI.getRepository();
        if (repository.queryByUserId(event.getSender().getUserId()) != null) {
            event.reply("您已绑定，请勿重复验证");
            return;
        }
        String code = msg.substring(3);
        UUID key = null;
        for (Map.Entry<UUID, AuthData> entry : LoginListener.DATA_HASH_MAP.entrySet()) {
            if (entry.getValue().getCode().equalsIgnoreCase(code)) {
                key = entry.getKey();
                final PlayerData data = new PlayerData();
                data.setUserId(event.getSender().getUserId());
                data.setUuid(key.toString());
                repository.save(data);
                break;
            }
        }
        if (key != null) {
            LoginListener.DATA_HASH_MAP.remove(key);
            event.reply(success);
        } else if (repository.queryByUserId(event.getSender().getUserId()) == null) {
            event.reply(failure);
        }
    }

    @EventHandler
    public void onLeave(GroupMemberQuitEvent event) {
        if (!StaticAPI.inGroups(event.getGroupId())) return;
        if (!removeOnLeave) return;
        final PlayerData delete = StaticAPI.getRepository().delete(event.getUserId());
        if (delete != null) {
            DefenceManager.getInstance().getLogger().info(PluginUtil.parseColor(
                    String.format("&c&l> [群%d]成员(%d)已退出，其绑定玩家(%s)数据被删除", event.getGroupId(), event.getUserId(), delete.getOfflinePlayer().getName())));
        }
    }

}
