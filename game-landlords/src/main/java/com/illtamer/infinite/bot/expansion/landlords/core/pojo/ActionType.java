package com.illtamer.infinite.bot.expansion.landlords.core.pojo;

/**
 * 行动类型
 * */
public enum ActionType {

    SINGLE("单张", 0),

    PAIR("对子", 0),

    THREE("三张", 0),

    BOMB("炸弹", 1),

    THREE_WITH_ONE("三带一", 0),

    THREE_WITH_PAIR("三带一对", 0),

    FOUR_WITH_TWO("四带二", 0),

    STRAIGHT("顺子", 0),

    STRAIGHT_PAIR("连对", 0),

    AIRCRAFT("飞机", 0),

    AIRCRAFT_WITH_WINGS("飞机带翅膀", 0),

    JOKER_BOMB("王炸", 2);

    /**
     * 行动名称
     * */
    private final String name;

    /**
     * 行动等级
     * */
    private final int level;

    ActionType(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }
}
