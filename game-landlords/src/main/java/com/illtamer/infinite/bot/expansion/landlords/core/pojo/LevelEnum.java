package com.illtamer.infinite.bot.expansion.landlords.core.pojo;

import org.jetbrains.annotations.Nullable;

public enum LevelEnum {

    _A(0, 12, "A"),

    _2(1, 13, "2"),

    _3(2, 1, "3"),

    _4(3, 2, "4"),

    _5(4, 3, "5"),

    _6(5, 4, "6"),

    _7(6, 5, "7"),

    _8(7, 6, "8"),

    _9(8, 7, "9"),

    _10(9, 8, "10"),

    _J(10, 9, "J"),

    _Q(11, 10, "Q"),

    _K(12, 11, "K"),

    _joker(13, 14, "小王"),

    _JOKER(14, 15, "大王");

    private final int index;

    private final int level;

    private final String name;

    LevelEnum(int index, int level, String name) {
        this.index = index;
        this.level = level;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public static LevelEnum getLevelEnum(int index) {
        if (index < 0 || index >= 15)
            throw new IllegalArgumentException(String.valueOf(index));
        return LevelEnum.values()[index];
    }

    @Nullable
    public static LevelEnum parse(String s) {
        for (LevelEnum levelEnum : LevelEnum.values()) {
            if (levelEnum.name.equals(s)) return levelEnum;
        }
        return null;
    }

}
