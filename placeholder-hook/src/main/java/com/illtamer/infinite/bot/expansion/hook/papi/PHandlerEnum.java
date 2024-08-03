package com.illtamer.infinite.bot.expansion.hook.papi;

public class PHandlerEnum {

    private final String subId;
    private final PHandler pHandler;

    public PHandlerEnum(String subId, PHandler pHandler) {
        this.subId = subId;
        this.pHandler = pHandler;
    }

    public String getSubId() {
        return subId;
    }

    public PHandler getPHandler() {
        return pHandler;
    }

}
