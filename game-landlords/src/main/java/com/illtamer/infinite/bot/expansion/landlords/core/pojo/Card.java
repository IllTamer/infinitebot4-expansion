package com.illtamer.infinite.bot.expansion.landlords.core.pojo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 卡牌实体类
 * */
public class Card implements Comparable<Card> {

    private Integer id;

    /**
     * 花色
     *
     * 大小王无花色
     * */
    @Nullable
    private SuitEnum suit;

    private LevelEnum level;

    public Card() {
    }

    public Card(Integer id, @Nullable SuitEnum suit, LevelEnum level) {
        this.id = id;
        this.suit = suit;
        this.level = level;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Nullable
    public SuitEnum getSuit() {
        return suit;
    }

    public void setSuit(@Nullable SuitEnum suit) {
        this.suit = suit;
    }

    public LevelEnum getLevel() {
        return level;
    }

    public void setLevel(LevelEnum level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return level.getName() +
                (suit == null ? "" : "[" + suit.getSuit() + "]") +
                '(' + id + ')';
    }

    // compare by level & suit
    @Override
    public int compareTo(@NotNull Card o) {
        if (this == o) return 0;
        final int levelCompare = Integer.compare(o.level.getLevel(), level.getLevel());
        if (levelCompare != 0) return levelCompare;
        if (o.suit == null) return 1;
        if (suit == null) return -1;
        return Integer.compare(o.suit.getIndex(), suit.getIndex());
    }

}
