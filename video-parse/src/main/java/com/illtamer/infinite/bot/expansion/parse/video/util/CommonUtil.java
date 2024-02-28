package com.illtamer.infinite.bot.expansion.parse.video.util;

import java.text.DecimalFormat;

public class CommonUtil {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    /**
     * 字节计数
     * */
    public static String countSize(long bytesLength) {
        double mb = (double) bytesLength / (1024 * 1024);
        return DF.format(mb) + "MB";
    }

    /**
     * 数值转换
     * 10001 -> 1.00万
     * */
    public static String countNum(long number) {
        if (number < 100) {
            return String.valueOf(number);
        }
        return String.format("%.2f", (double) number / 10000) + "万";
    }

}
