package com.illtamer.infinite.bot.expansion.manager.login.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * 待验证的登录请求
 * */
@Data
@AllArgsConstructor
public class PendingRequest {

    /**
     * 触发验证的玩家名（用于展示）
     * */
    private String playerName;

    /**
     * 触发验证的玩家 UUID
     * */
    private UUID uuid;

    /**
     * 请求过期时间戳（毫秒）
     * */
    private long expireAt;

}
