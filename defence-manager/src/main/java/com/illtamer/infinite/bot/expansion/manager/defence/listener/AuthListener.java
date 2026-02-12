package com.illtamer.infinite.bot.expansion.manager.defence.listener;

import com.illtamer.infinite.bot.expansion.manager.defence.DefenceManager;
import com.illtamer.infinite.bot.expansion.manager.defence.entity.AuthData;
import com.illtamer.infinite.bot.expansion.manager.defence.util.AuthUtil;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.repository.PlayerDataRepository;
import com.illtamer.infinite.bot.minecraft.util.Lambda;
import com.illtamer.infinite.bot.minecraft.util.PluginUtil;
import com.illtamer.perpetua.sdk.Pair;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.event.notice.group.GroupMemberQuitEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class AuthListener implements Listener {

    private final int authType;
    private final boolean removeOnLeave;
    private final String success;
    private final String failure;
    private final Language language;

    public AuthListener(ExpansionConfig configFile, Language language) {
        final FileConfiguration config = configFile.getConfig();
        this.authType = config.getInt("auth-type");
        this.removeOnLeave = config.getBoolean("remove-on-leave");
        this.success = config.getString("success");
        this.failure = config.getString("failure");
        this.language = language;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAuth(MessageEvent event) {
        String msg = event.getRawMessage().trim();
        if (msg.startsWith("清空缓存 ") && StaticAPI.isAdmin(event.getSender().getUserId())) {
            int value = Integer.parseInt(msg.split(" ")[1]);
            LoginListener.clearCommon(value);
            event.reply("Done-");
            event.setCancelled(true);
            return;
        }
        final PlayerData data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());

        if (LoginListener.getDATA_HASH_MAP().isEmpty()) {
            return;
        }
        if (!AuthUtil.isCodeRegx(msg)) {
            return;
        }
        event.setCancelled(true);

        if (authType == 1) {
            if (data != null && data.getValidUUID() != null) {
                event.reply(language.get("auth", "exist-valid"));
                return;
            }
            doAuth(msg, event.getUserId(), data, true, event::reply);
        } else if (authType == 0) {
            if (data != null && data.getUuid() != null) {
                event.reply(language.get("auth", "exist-offline"));
                return;
            }
            doAuth(msg, event.getUserId(), data, false, event::reply);
        }
    }

    @EventHandler
    public void onLeave(GroupMemberQuitEvent event) {
        if (!StaticAPI.inGroups(event.getGroupId())) return;
        if (!removeOnLeave) return;
        final PlayerData delete = StaticAPI.getRepository().delete(event.getUserId());
        if (delete != null) {
            DefenceManager.getInstance().getLogger().info(PluginUtil.parseColor(
                    String.format(language.get("auth", "leave"), event.getGroupId(), event.getUserId(), Lambda.nullableInvoke(OfflinePlayer::getName, delete.getOfflinePlayer()))));
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
            reply.accept(language.get("auth", "except-type").replace("%type%", (valid ? "正版" : "离线")));
            return;
        }
        PlayerData data = record == null ? new PlayerData() : record;
        if (data.getUserId() != null && !data.getUserId().equals(userId)) {
            reply.accept(language.get("auth", "except-type").replace("%type%", (valid ? "正版验证" : "离线验证")));
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
