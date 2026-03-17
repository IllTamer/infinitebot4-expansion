package com.illtamer.infinite.bot.expansion.manager.basic.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录登出事件数据
 */
@Data
public class LoginOutData implements Serializable {

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

    public LoginOutData(String clientName) {
        this.clientName = clientName;
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