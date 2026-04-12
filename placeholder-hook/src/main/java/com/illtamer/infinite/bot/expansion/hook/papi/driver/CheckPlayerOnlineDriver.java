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
import org.bukkit.scheduler.BukkitTask;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class CheckPlayerOnlineDriver extends AbstractDistributedListener<OnlineData> {

    private static final long REFRESH_INTERVAL_TICKS = 60L;
    private static final long REQUEST_TIMEOUT_MILLIS = 2500L;
    private static final long MANUAL_TARGET_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

    private static final String CONTEXT_TARGETS = "targets";

    private final Map<String, QueryTarget> manualTargets = new ConcurrentHashMap<>();
    private final Map<String, Long> manualTargetAccess = new ConcurrentHashMap<>();
    private final Map<String, Boolean> onlineCache = new ConcurrentHashMap<>();
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private volatile BukkitTask refreshTask;

    public CheckPlayerOnlineDriver(IExpansion expansion) {
        super(expansion, OnlineData.class);
    }

    public void startRefreshTask() {
        if (refreshTask != null) {
            return;
        }
        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                BukkitBootstrap.getInstance(),
                this::refreshOnlineCache,
                20L,
                REFRESH_INTERVAL_TICKS
        );
    }

    public void stopRefreshTask() {
        BukkitTask task = refreshTask;
        refreshTask = null;
        if (task != null) {
            task.cancel();
        }
        manualTargets.clear();
        manualTargetAccess.clear();
        onlineCache.clear();
        refreshing.set(false);
    }

    public boolean isPlayerOnline(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return false;
        }

        final String key = offlinePlayer.getUniqueId().toString();
        QueryTarget target = manualTargets.get(key);
        if (target == null) {
            target = buildTargetForManualTrack(offlinePlayer);
            manualTargets.put(key, target);
        }
        manualTargetAccess.put(key, System.currentTimeMillis());

        return Boolean.TRUE.equals(onlineCache.get(key));
    }

    private QueryTarget buildTargetForManualTrack(OfflinePlayer offlinePlayer) {
        final String key = offlinePlayer.getUniqueId().toString();
        final String name = offlinePlayer.getName();

        String bindUuid = null;
        String validUuid = null;
        try {
            PlayerData bindData = BotConfiguration.getInstance().getRepository().queryByUUID(offlinePlayer.getUniqueId());
            if (bindData != null) {
                bindUuid = bindData.getUuid();
                validUuid = bindData.getValidUUID();
            }
        } catch (Exception e) {
            log.warn("[CheckPlayerOnline] 加载玩家绑定信息失败, uuid={}", key, e);
        }

        return new QueryTarget(key, key, bindUuid, validUuid, name);
    }

    private QueryTarget buildTargetFromOfflineSnapshot(OfflinePlayer offlinePlayer) {
        final String key = offlinePlayer.getUniqueId().toString();
        return new QueryTarget(key, key, null, null, offlinePlayer.getName());
    }

    private void refreshOnlineCache() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }

        try {
            cleanupManualTargets();

            Map<String, QueryTarget> snapshot = buildSnapshotTargets();
            if (snapshot.isEmpty()) {
                onlineCache.clear();
                return;
            }

            Set<String> onlineKeys;
            try {
                onlineKeys = queryDistributedOnlineKeys(snapshot);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (TimeoutException e) {
                log.warn("[CheckPlayerOnline] 3s 批量在线状态刷新超时");
                return;
            } catch (ExecutionException e) {
                log.error("[CheckPlayerOnline] 3s 批量在线状态刷新失败", e);
                return;
            }

            mergeCacheWithSnapshot(snapshot.keySet(), onlineKeys);
        } finally {
            refreshing.set(false);
        }
    }

    private Map<String, QueryTarget> buildSnapshotTargets() {
        Map<String, QueryTarget> snapshot = new LinkedHashMap<>();

        for (OfflinePlayer offlinePlayer : getOfflinePlayersSnapshot()) {
            if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
                continue;
            }
            QueryTarget target = buildTargetFromOfflineSnapshot(offlinePlayer);
            snapshot.put(target.getKey(), target);
        }

        snapshot.putAll(manualTargets);
        return snapshot;
    }

    private OfflinePlayer[] getOfflinePlayersSnapshot() {
        if (Bukkit.isPrimaryThread()) {
            return Bukkit.getOfflinePlayers();
        }

        CompletableFuture<OfflinePlayer[]> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> {
            try {
                future.complete(Bukkit.getOfflinePlayers());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            return future.get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new OfflinePlayer[0];
        } catch (ExecutionException | TimeoutException e) {
            log.warn("[CheckPlayerOnline] 获取 Bukkit 离线玩家快照失败", e);
            return new OfflinePlayer[0];
        }
    }

    private void cleanupManualTargets() {
        long expireBefore = System.currentTimeMillis() - MANUAL_TARGET_TTL_MILLIS;
        List<String> staleKeys = new ArrayList<>();
        for (Map.Entry<String, Long> entry : manualTargetAccess.entrySet()) {
            if (entry.getValue() == null || entry.getValue() < expireBefore) {
                staleKeys.add(entry.getKey());
            }
        }
        for (String staleKey : staleKeys) {
            manualTargetAccess.remove(staleKey);
            manualTargets.remove(staleKey);
        }
    }

    private Set<String> queryDistributedOnlineKeys(Map<String, QueryTarget> snapshot)
            throws InterruptedException, ExecutionException, TimeoutException {
        DistributedEventContext context = new DistributedEventContext();
        context.setParam(CONTEXT_TARGETS, new ArrayList<>(snapshot.values()));

        CompletableFuture<Set<String>> future = new CompletableFuture<>();
        getProcessor().tryProcessEvent(getIdentifier(), context, result -> {
            Set<String> onlineKeys = new HashSet<>();
            if (result != null && result.getDataList() != null) {
                for (OnlineData data : result.getDataList()) {
                    if (data == null || data.getOnlineKeys() == null) {
                        continue;
                    }
                    onlineKeys.addAll(data.getOnlineKeys());
                }
            }
            future.complete(onlineKeys);
        }, future::completeExceptionally);

        return future.get(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void mergeCacheWithSnapshot(Set<String> snapshotKeys, Set<String> onlineKeys) {
        onlineCache.keySet().removeIf(key -> !snapshotKeys.contains(key));
        for (String key : snapshotKeys) {
            onlineCache.put(key, onlineKeys.contains(key));
        }
    }

    @Override
    public OnlineData handle(DistributedEventContext context) {
        OnlineData result = new OnlineData();

        List<QueryTarget> targets = parseTargets(context.getParam(CONTEXT_TARGETS));
        if (targets.isEmpty()) {
            return result;
        }

        Set<String> onlineKeys = collectOnlineKeysOnCurrentServer(targets);
        result.setOnlineKeys(onlineKeys);
        return result;
    }

    private List<QueryTarget> parseTargets(Object rawTargets) {
        if (!(rawTargets instanceof List)) {
            return Collections.emptyList();
        }

        List<?> list = (List<?>) rawTargets;
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<QueryTarget> targets = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof QueryTarget) {
                targets.add((QueryTarget) item);
                continue;
            }

            if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                String key = asString(map.get("key"));
                if (!isNotBlank(key)) {
                    continue;
                }
                targets.add(new QueryTarget(
                        key,
                        asString(map.get("uuid")),
                        asString(map.get("bindUuid")),
                        asString(map.get("validUuid")),
                        asString(map.get("name"))
                ));
            }
        }

        return targets;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Set<String> collectOnlineKeysOnCurrentServer(List<QueryTarget> targets) {
        if (targets.isEmpty()) {
            return Collections.emptySet();
        }

        if (Bukkit.isPrimaryThread()) {
            return collectOnlineKeysNow(targets);
        }

        CompletableFuture<Set<String>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(BukkitBootstrap.getInstance(), () -> {
            try {
                future.complete(collectOnlineKeysNow(targets));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptySet();
        } catch (ExecutionException e) {
            log.error("[CheckPlayerOnline] 节点在线状态批量匹配失败", e);
            return Collections.emptySet();
        }
    }

    private Set<String> collectOnlineKeysNow(List<QueryTarget> targets) {
        Collection<? extends Player> onlinePlayers = BukkitBootstrap.getInstance().getServer().getOnlinePlayers();

        Set<String> onlineUuids = new HashSet<>();
        Set<String> onlineNamesLower = new HashSet<>();
        for (Player player : onlinePlayers) {
            onlineUuids.add(player.getUniqueId().toString());
            if (isNotBlank(player.getName())) {
                onlineNamesLower.add(player.getName().toLowerCase(Locale.ROOT));
            }
        }

        Set<String> onlineKeys = new HashSet<>();
        for (QueryTarget target : targets) {
            if (target == null || !isNotBlank(target.getKey())) {
                continue;
            }

            boolean matched = matchesAnyIdentifier(target, onlineUuids, onlineNamesLower);
            if (matched) {
                onlineKeys.add(target.getKey());
            }
        }
        return onlineKeys;
    }

    private boolean matchesAnyIdentifier(QueryTarget target, Set<String> onlineUuids, Set<String> onlineNamesLower) {
        if (isNotBlank(target.getUuid()) && onlineUuids.contains(target.getUuid())) {
            return true;
        }
        if (isNotBlank(target.getBindUuid()) && onlineUuids.contains(target.getBindUuid())) {
            return true;
        }
        if (isNotBlank(target.getValidUuid()) && onlineUuids.contains(target.getValidUuid())) {
            return true;
        }
        if (isNotBlank(target.getName())) {
            return onlineNamesLower.contains(target.getName().toLowerCase(Locale.ROOT));
        }
        return false;
    }

    private boolean isNotBlank(String value) {
        return StringUtil.isNotBlank(value);
    }

    @Override
    public String getIdentifier() {
        return "isPlayerOnline";
    }

    public static class QueryTarget implements Serializable {

        private static final long serialVersionUID = 1L;

        private String key;
        private String uuid;
        private String bindUuid;
        private String validUuid;
        private String name;

        public QueryTarget() {
        }

        public QueryTarget(String key, String uuid, String bindUuid, String validUuid, String name) {
            this.key = key;
            this.uuid = uuid;
            this.bindUuid = bindUuid;
            this.validUuid = validUuid;
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getBindUuid() {
            return bindUuid;
        }

        public void setBindUuid(String bindUuid) {
            this.bindUuid = bindUuid;
        }

        public String getValidUuid() {
            return validUuid;
        }

        public void setValidUuid(String validUuid) {
            this.validUuid = validUuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

}
