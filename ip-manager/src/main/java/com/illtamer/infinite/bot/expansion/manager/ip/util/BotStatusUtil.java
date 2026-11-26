package com.illtamer.infinite.bot.expansion.manager.ip.util;

import com.illtamer.infinite.bot.minecraft.configuration.StatusCheckRunner;
import com.illtamer.perpetua.sdk.websocket.OneBotConnection;

/**
 * 本体机器人链接可用性判定
 * */
public class BotStatusUtil {

    /**
     * 机器人是否可用：WebSocket 链接在线，且账号状态检查正常
     * */
    public static boolean isBotAvailable() {
        return OneBotConnection.isRunning() && StatusCheckRunner.getLoginInfo() != null;
    }

    /**
     * 便于日志输出的连接状态摘要
     * */
    public static String describe() {
        return "running=" + OneBotConnection.isRunning()
                + ", loginInfo=" + (StatusCheckRunner.getLoginInfo() != null ? "ok" : "null");
    }

    private BotStatusUtil() {}
}
