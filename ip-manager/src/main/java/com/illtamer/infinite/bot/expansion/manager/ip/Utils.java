package com.illtamer.infinite.bot.expansion.manager.ip;

import java.util.Random;

public class Utils {

    private static final Random random = new Random();
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String getCode() {
        StringBuilder stringBuilder = new StringBuilder();
        char randomLetter = LETTERS.charAt(random.nextInt(LETTERS.length()));
        stringBuilder.append(randomLetter);
        for (int x = 0; x < 5; x++) {
            stringBuilder.append(random.nextInt(10));
        }
        return stringBuilder.toString();
    }

    public static String encodeQQ(String qq) {
        StringBuilder stringBuilder = new StringBuilder();
        int b = 0;
        for (int a = 0; a < qq.length(); a++) {
            char c = qq.charAt(a);
            b++;
            if (b > 3 && b < 8) {
                stringBuilder.append("*");
            } else {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

}
