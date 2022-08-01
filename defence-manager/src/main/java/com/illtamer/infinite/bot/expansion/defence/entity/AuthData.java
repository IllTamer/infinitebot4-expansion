package com.illtamer.infinite.bot.expansion.defence.entity;

public class AuthData {

    private final String code;
    private final String time;

    public AuthData(String code, String time) {
        this.code = code;
        this.time = time;
    }

    public String getCode() {
        return code;
    }

    public String getTime() {
        return time;
    }

}
