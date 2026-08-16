package com.illtamer.infinite.bot.expansion.manager.login.service;

import com.illtamer.infinite.bot.expansion.manager.login.LoginManager;
import com.illtamer.perpetua.sdk.entity.transfer.entity.Friend;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 机器人好友列表缓存
 * <p>
 * 通过 {@link OpenAPIHandling#getFriendList()} 获取好友并按 TTL 缓存。
 * 出错时保留旧缓存，避免因偶发 API 失败误判"非好友"。
 * */
public class FriendCache {

    private final long ttlMillis;
    private final Object lock = new Object();

    private volatile Set<Long> cache = Collections.emptySet();
    private volatile long refreshAt = 0L;
    private volatile boolean initialized = false;

    public FriendCache(long ttlSeconds) {
        this.ttlMillis = Math.max(30L, ttlSeconds) * 1000L;
    }

    /**
     * 查询指定 QQ 是否为机器人好友
     * <p>
     * 若还未初始化且 API 抛异常，返回 {@code false}（走"不确定→不校验"路径，避免误伤所有玩家）。
     * */
    public boolean isFriend(long userId) {
        ensureFresh();
        return cache.contains(userId);
    }

    /**
     * 强制刷新缓存
     * */
    public void refresh() {
        synchronized (lock) {
            doRefresh();
        }
    }

    private void ensureFresh() {
        if (initialized && System.currentTimeMillis() < refreshAt) {
            return;
        }
        synchronized (lock) {
            if (initialized && System.currentTimeMillis() < refreshAt) {
                return;
            }
            doRefresh();
        }
    }

    private void doRefresh() {
        try {
            List<Friend> friends = OpenAPIHandling.getFriendList();
            Set<Long> updated = new HashSet<>(friends.size());
            for (Friend friend : friends) {
                if (friend.getUserId() != null) {
                    updated.add(friend.getUserId());
                }
            }
            cache = updated;
            refreshAt = System.currentTimeMillis() + ttlMillis;
            initialized = true;
        } catch (Exception e) {
            LoginManager instance = LoginManager.getInstance();
            if (instance != null) {
                instance.getLogger().warn("刷新机器人好友列表失败，沿用旧缓存: " + e.getMessage());
            }
            // 出错时给旧缓存续 30 秒，避免每次登录都触发失败调用
            refreshAt = System.currentTimeMillis() + 30_000L;
        }
    }

}
