package com.illtamer.infinite.bot.expansion.landlords.core.pojo;

import java.util.List;

/**
 * 玩家实体类
 * */
public class Participant {

    /**
     * 玩家持有的牌
     * */
    private List<Card> cardList;

    /**
     * 是否是地主
     * */
    private boolean landlord;

    private long userId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public List<Card> getCardList() {
        return cardList;
    }

    public void setCardList(List<Card> cardList) {
        this.cardList = cardList;
    }

    public boolean isLandlord() {
        return landlord;
    }

    public void setLandlord(boolean landlord) {
        this.landlord = landlord;
    }
}
