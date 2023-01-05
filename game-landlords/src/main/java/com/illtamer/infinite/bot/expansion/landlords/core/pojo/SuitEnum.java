package com.illtamer.infinite.bot.expansion.landlords.core.pojo;

/**
 * 花色枚举
 * */
public enum SuitEnum {

    HEART(0, '♥'),

    SPADE(1, '♠'),

    DIAMOND(2, '♦'),

    CLUB(3, '♣');

    private final int index;

    private final char suit;

    SuitEnum(int index, char suit) {
        this.index = index;
        this.suit = suit;
    }

    public int getIndex() {
        return index;
    }

    public char getSuit() {
        return suit;
    }

    public static SuitEnum parse(int index) {
        if (index < 0 || index >= 4)
            throw new IllegalArgumentException(String.valueOf(index));
        return SuitEnum.values()[index];
    }

}
