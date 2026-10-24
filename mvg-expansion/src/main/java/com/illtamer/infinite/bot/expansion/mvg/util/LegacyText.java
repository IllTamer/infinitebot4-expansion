package com.illtamer.infinite.bot.expansion.mvg.util;

import org.bukkit.inventory.ItemRarity;

/**
 * {@code §} 文本工具
 * <p>
 * mvg 的 tooltip / playerlist 文本直接解析传统的 {@code §} 颜色与修饰符，
 * 因此附属生成中文 tooltip 时统一使用 {@code §} 码。
 * */
public final class LegacyText {

    public static final char SECTION = '\u00A7';

    public static final String BLACK = SECTION + "0";
    public static final String DARK_BLUE = SECTION + "1";
    public static final String DARK_GREEN = SECTION + "2";
    public static final String DARK_AQUA = SECTION + "3";
    public static final String DARK_RED = SECTION + "4";
    public static final String DARK_PURPLE = SECTION + "5";
    public static final String GOLD = SECTION + "6";
    public static final String GRAY = SECTION + "7";
    public static final String DARK_GRAY = SECTION + "8";
    public static final String BLUE = SECTION + "9";
    public static final String GREEN = SECTION + "a";
    public static final String AQUA = SECTION + "b";
    public static final String RED = SECTION + "c";
    public static final String LIGHT_PURPLE = SECTION + "d";
    public static final String YELLOW = SECTION + "e";
    public static final String WHITE = SECTION + "f";

    public static final String OBFUSCATED = SECTION + "k";
    public static final String BOLD = SECTION + "l";
    public static final String STRIKETHROUGH = SECTION + "m";
    public static final String UNDERLINE = SECTION + "n";
    public static final String ITALIC = SECTION + "o";
    public static final String RESET = SECTION + "r";

    private LegacyText() {}

    /**
     * 把 {@code &} 形式的颜色码转换为 {@code §}
     * <p>
     * 除传统的单字符码外，同时支持十六进制的两种常见写法：
     * {@code &#RRGGBB} 与 {@code &x&R&R&G&G&B&B}，统一转成原版/BungeeCord 的
     * {@code §x§R§R§G§G§B§B} 形式。
     * */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length() + 8);
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current != '&') {
                builder.append(current);
                index++;
                continue;
            }
            // &#RRGGBB（8 个字符）
            if (index + 7 < text.length() && text.charAt(index + 1) == '#'
                    && isHexDigits(text, index + 2, 6)) {
                builder.append(SECTION).append('x');
                for (int offset = 0; offset < 6; offset++) {
                    builder.append(SECTION).append(text.charAt(index + 2 + offset));
                }
                index += 8;
                continue;
            }
            // &x&R&R&G&G&B&B（14 个字符）
            if (index + 13 < text.length() && isHexChar(text.charAt(index + 1))
                    && isHexPairs(text, index + 2)) {
                builder.append(SECTION).append('x');
                for (int offset = 0; offset < 6; offset++) {
                    builder.append(SECTION).append(text.charAt(index + 3 + offset * 2));
                }
                index += 14;
                continue;
            }
            if (index + 1 < text.length() && isColorCode(text.charAt(index + 1))) {
                builder.append(SECTION).append(text.charAt(index + 1));
                index += 2;
                continue;
            }
            builder.append(current);
            index++;
        }
        return builder.toString();
    }

    private static boolean isHexChar(char c) {
        return c == 'x' || c == 'X';
    }

    /**
     * 校验从 {@code from} 开始是否为 {@code length} 个十六进制数字
     * */
    private static boolean isHexDigits(String text, int from, int length) {
        if (from + length > text.length()) {
            return false;
        }
        for (int index = from; index < from + length; index++) {
            if (Character.digit(text.charAt(index), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验从 {@code from} 开始是否为 {@code &R&R&G&G&B&B} 形式的 6 组十六进制数字
     * */
    private static boolean isHexPairs(String text, int from) {
        int index = from;
        for (int pair = 0; pair < 6; pair++) {
            if (index + 1 >= text.length() || text.charAt(index) != '&'
                    || Character.digit(text.charAt(index + 1), 16) < 0) {
                return false;
            }
            index += 2;
        }
        return true;
    }

    public static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'k' && c <= 'o')
                || c == 'r'
                || (c >= 'A' && c <= 'F')
                || (c >= 'K' && c <= 'O')
                || c == 'R';
    }

    /**
     * 物品稀有度对应的强调色码，与原版 {@code Rarity#getStyle} 一致
     * */
    public static String rarityColor(ItemRarity rarity) {
        if (rarity == null) {
            return WHITE;
        }
        switch (rarity) {
            case UNCOMMON:
                return YELLOW;
            case RARE:
                return AQUA;
            case EPIC:
                return LIGHT_PURPLE;
            default:
                return WHITE;
        }
    }

    /**
     * 按稀有度着色，已包含颜色码的文本（如自定义名）保持原样
     * */
    public static String withRarity(String text, ItemRarity rarity) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (containsStyle(text)) {
            return text;
        }
        return rarityColor(rarity) + text;
    }

    public static boolean containsStyle(String text) {
        return text.indexOf(SECTION) >= 0 || text.indexOf('&') >= 0;
    }

    /**
     * 去除所有 {@code §} 样式码，得到纯文本
     * */
    public static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == SECTION && i + 1 < chars.length) {
                i++;
                continue;
            }
            builder.append(chars[i]);
        }
        return builder.toString();
    }

}
