package com.illtamer.infinite.bot.expansion.defence.entity;

public class AuthData {

    private final String code;
    private final String time;
    private final boolean valid;

    public AuthData(String code, String time, boolean valid) {
        this.code = code;
        this.time = time;
        this.valid = valid;
    }

    public String getCode() {
        return code;
    }

    public String getTime() {
        return time;
    }

    public boolean isValid() {
        return valid;
    }

}
