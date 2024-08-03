package com.illtamer.infinite.bot.expansion.hook.papi;

public class StringHelper {

    public static Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception ignore) {
            return null;
        }
    }

}
