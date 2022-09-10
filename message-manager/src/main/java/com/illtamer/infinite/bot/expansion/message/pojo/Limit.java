package com.illtamer.infinite.bot.expansion.message.pojo;

public class Limit {

    /**
     * 每分钟该消息最大触发次数
     * */
    private int maxPerMinute;

    /**
     * 每个玩家单次触发间隔 (单位：秒)
     * */
    private int userTriggerInterval;

    public int getMaxPerMinute() {
        return maxPerMinute;
    }

    public void setMaxPerMinute(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public int getUserTriggerInterval() {
        return userTriggerInterval;
    }

    public void setUserTriggerInterval(int userTriggerInterval) {
        this.userTriggerInterval = userTriggerInterval;
    }

}
