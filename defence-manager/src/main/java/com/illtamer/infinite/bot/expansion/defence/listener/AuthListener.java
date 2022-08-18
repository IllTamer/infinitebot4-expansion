package com.illtamer.infinite.bot.expansion.defence.listener;

import com.illtamer.infinite.bot.api.Pair;
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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

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
            if (msg.startsWith("清空缓存 ") && StaticAPI.isAdmin(event.getSender().getUserId())) {
                int value = Integer.parseInt(msg.split(" ")[1]);
                LoginListener.clearCommon(value);
                event.reply("Done-");
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        msg = msg.substring("验证 ".length());
        final PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());

        if (msg.startsWith("正版 ")) {
            if (data != null && data.getValidUUID() != null) {
                event.reply("您已绑定正版账号，请勿重复验证");
                return;
            }
            String code = msg.substring("正版 ".length());
            doAuth(code, event.getUserId(), data, true, event::reply);
        } else if (msg.startsWith("离线 ")) {
            if (data != null && data.getUuid() != null) {
                event.reply("您已绑定离线账号，请勿重复验证");
                return;
            }
            String code = msg.substring("离线 ".length());
            doAuth(code, event.getUserId(), data, false, event::reply);
        } else {
            event.reply("请核实格式 '验证 正版/离线 <code>'");
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

    private void doAuth(String code, long userId, @Nullable PlayerData record, boolean valid, Consumer<String> reply) {
        final Pair<UUID, AuthData> pair = LoginListener.getByCode(code);
        if (pair == null) {
            reply.accept(failure);
            return;
        }
        final AuthData authData = pair.getValue();
        final String uuid = pair.getKey().toString();
        if ((valid && !authData.isValid()) || (!valid && authData.isValid())) {
            reply.accept("账号类型不符，要求类型：" + (valid ? "正版" : "离线"));
            return;
        }
        PlayerData data = record == null ? new PlayerData() : record;
        if (data.getUserId() != null && !data.getUserId().equals(userId)) {
            reply.accept("请使用原先角色绑定的QQ账号进行" + (valid ? "正版" : "离线") + "绑定");
            return;
        }
        LoginListener.removeByUUID(pair.getKey());
        data.setUserId(userId);
        if (valid)
            data.setValidUUID(uuid);
        else
            data.setUuid(uuid);
        final PlayerDataRepository repository = StaticAPI.getRepository();
        if (record != null) {
            repository.update(data);
        } else {
            repository.save(data);
        }
        reply.accept(success);
    }

}
