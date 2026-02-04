package com.illtamer.infinite.bot.expansion.manager.basic.distribute;

import lombok.Data;

import java.util.List;
import java.util.ArrayList;

/**
 * 登录登出事件数据
 */
@Data
public class LoginOutData {

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 执行消息
     */
    private String message;

    /**
     * 踢出的玩家列表
     */
    private List<String> kickedPlayers = new ArrayList<>();

    /**
     * 失败的踢出操作
     */
    private List<String> failedKicks = new ArrayList<>();

    /**
     * 客户端名称
     */
    private String clientName;

    public LoginOutData() {}

    public LoginOutData(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginOutData(String clientName) {
        this.clientName = clientName;
    }

    /**
     * 创建成功结果
     */
    public static LoginOutData success(String message) {
        return new LoginOutData(true, message);
    }

    /**
     * 创建失败结果
     */
    public static LoginOutData failure(String message) {
        return new LoginOutData(false, message);
    }

    /**
     * 添加踢出玩家
     */
    public LoginOutData addKickedPlayer(String playerName) {
        this.kickedPlayers.add(playerName);
        return this;
    }

    /**
     * 添加失败踢出
     */
    public LoginOutData addFailedKick(String playerName) {
        this.failedKicks.add(playerName);
        return this;
    }
}