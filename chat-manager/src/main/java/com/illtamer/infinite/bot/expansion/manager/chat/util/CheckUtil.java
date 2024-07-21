package com.illtamer.infinite.bot.expansion.manager.chat.util;

public class CheckUtil {

    public static boolean isNum(String numStr) {
        try {
            Integer.valueOf(numStr);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

}
