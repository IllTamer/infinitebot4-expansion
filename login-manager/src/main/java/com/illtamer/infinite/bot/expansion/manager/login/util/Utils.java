package com.illtamer.infinite.bot.expansion.manager.login.util;

/**
 * 通用工具方法
 * */
public class Utils {

    private Utils() {}

    /**
     * 对 QQ 号做部分遮罩，用于展示
     * <p>
     * 12345678 → 123**678
     * */
    public static String encodeQQ(String qq) {
        if (qq == null || qq.length() == 0) return "";
        StringBuilder builder = new StringBuilder();
        int b = 0;
        for (int a = 0; a < qq.length(); a++) {
            char c = qq.charAt(a);
            b++;
            if (b > 3 && b < 8) {
                builder.append('*');
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * 判断是否为内网 / 回环 / 链路本地 IP
     * */
    public static boolean isLocalIp(String ip) {
        if (ip == null || ip.length() == 0) return true;
        // IPv6 回环 / 链路本地
        if ("::1".equals(ip) || ip.startsWith("fe80:") || ip.startsWith("fc") || ip.startsWith("fd")) {
            return true;
        }
        // IPv4
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        int first;
        int second;
        try {
            first = Integer.parseInt(parts[0]);
            second = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (first == 127 || first == 10) return true;
        if (first == 192 && second == 168) return true;
        if (first == 172 && second >= 16 && second <= 31) return true;
        if (first == 169 && second == 254) return true;
        return false;
    }

}
