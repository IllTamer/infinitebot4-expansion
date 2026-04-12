package com.illtamer.infinite.bot.expansion.hook.papi.driver;

import com.illtamer.infinite.bot.expansion.hook.papi.context.OnlineData;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.api.distribute.AbstractDistributedListener;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedEventContext;
import com.illtamer.infinite.bot.minecraft.configuration.config.BotConfiguration;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import com.illtamer.infinite.bot.minecraft.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class CheckPlayerOnlineDriver extends AbstractDistributedListener<OnlineData> {

    private static final long REQUEST_TIMEOUT_MILLIS = 2500L;

    public CheckPlayerOnlineDriver(IExpansion expansion) {
        super(expansion, OnlineData.class);
    }

    public boolean isPlayerOnline(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return false;
        }

        final String uuid = offlinePlayer.getUniqueId().toString();
        DistributedEventContext context = new DistributedEventContext();
        context.setParam("uuid", uuid);
        if (StringUtil.isNotBlank(offlinePlayer.getName())) {
            context.setParam("name", offlinePlayer.getName());
        }

        PlayerData bindData = BotConfiguration.getInstance().getRepository().queryByUUID(offlinePlayer.getUniqueId());
        if (bindData != null) {
            if (StringUtil.isNotBlank(bindData.getUuid())) {
                context.setParam("bindUuid", bindData.getUuid());
            }
            if (StringUtil.isNotBlank(bindData.getValidUUID())) {
                context.setParam("validUuid", bindData.getValidUUID());
            }
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        getProcessor().tryProcessEvent(getIdentifier(), context, result -> {
            boolean online = result.getDataList().stream().anyMatch(data -> Boolean.TRUE.equals(data.getOnline()));
            future.complete(online);
        }, e -> {
            log.error("[CheckPlayerOnline] 分布式查询在线状态失败, uuid={}", uuid, e);
            future.complete(false);
        });

        try {
            return future.get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (TimeoutException e) {
            log.warn("[CheckPlayerOnline] 分布式查询超时, uuid={}", uuid);
            return false;
        } catch (ExecutionException e) {
            log.error("[CheckPlayerOnline] 分布式查询执行失败, uuid={}", uuid, e);
            return false;
        }
    }

    @Override
    public OnlineData handle(DistributedEventContext context) {
        OnlineData result = new OnlineData();

        final String uuid = context.getParam("uuid");
        final String bindUuid = context.getParam("bindUuid");
        final String validUuid = context.getParam("validUuid");
        final String name = context.getParam("name");

        result.setOnline(isOnlineByName(name)
                || isOnlineByUUID(uuid)
                || isOnlineByUUID(bindUuid)
                || isOnlineByUUID(validUuid));
        return result;
    }

    private boolean isOnlineByName(String name) {
        if (!StringUtil.isNotBlank(name)) {
            return false;
        }
        Player player = Bukkit.getPlayerExact(name);
        return player != null && player.isOnline();
    }

    private boolean isOnlineByUUID(String uuid) {
        if (!StringUtil.isNotBlank(uuid)) {
            return false;
        }
        try {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            return player != null && player.isOnline();
        } catch (IllegalArgumentException ignore) {
            return false;
        }
    }

    @Override
    public String getIdentifier() {
        return "isPlayerOnline";
    }

}
