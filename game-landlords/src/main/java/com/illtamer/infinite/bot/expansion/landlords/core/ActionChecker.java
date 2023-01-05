package com.illtamer.infinite.bot.expansion.landlords.core;

import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.ActionType;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.Card;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.LevelEnum;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ActionChecker {

    private static final int JOKER_BOMB_VALUE = LevelEnum._joker.getLevel() + LevelEnum._JOKER.getLevel();

    @Nullable
    public static ActionType getActionType(List<Card> cardList) {
        ActionType type = null;
        if (cardList != null && cardList.size() != 0) {
            if (isSingle(cardList)) {
                type = ActionType.SINGLE;
            } else if (isPair(cardList)) {
                type = ActionType.PAIR;
            } else if (isThree(cardList)) {
                type = ActionType.THREE;
            } else if (isThreeWithOne(cardList)) {
                type = ActionType.THREE_WITH_ONE;
            } else if (isThreeWithPair(cardList)) {
                type = ActionType.THREE_WITH_PAIR;
            } else if (isStraight(cardList)) {
                type = ActionType.STRAIGHT;
            } else if (isStraightPair(cardList)) {
                type = ActionType.STRAIGHT_PAIR;
            } else if (isFourWithTwo(cardList)) {
                type = ActionType.FOUR_WITH_TWO;
            } else if (isBomb(cardList)) {
                type = ActionType.BOMB;
            } else if (isJokerBomb(cardList)) {
                type = ActionType.JOKER_BOMB;
            } else if (isAircraft(cardList)) {
                type = ActionType.AIRCRAFT;
            } else if (isAircraftWithWing(cardList)) {
                type = ActionType.AIRCRAFT_WITH_WINGS;
            }
        }
        return type;
    }

    private static boolean isSingle(List<Card> cardList) {
        return cardList.size() == 1;
    }

    private static boolean isPair(List<Card> cardList) {
        if (cardList.size() != 2) return false;
        return isAllLevelEquals(cardList);
    }

    private static boolean isThree(List<Card> cardList) {
        if (cardList.size() != 3) return false;
        return isAllLevelEquals(cardList);
    }

    private static boolean isBomb(List<Card> cardList) {
        if (cardList.size() != 4) return false;
        return isAllLevelEquals(cardList);
    }

    private static boolean isThreeWithOne(List<Card> cardList) {
        return cardList.size() == 4 && checkGroup(1, 3, 2, cardList);
    }

    private static boolean isThreeWithPair(List<Card> cardList) {
        return cardList.size() == 5 && checkGroup(2, 3, 2, cardList);
    }

    private static boolean isFourWithTwo(List<Card> cardList) {
        return cardList.size() == 6 && checkGroup(2, 4, 2, cardList);
    }

    private static boolean isStraight(List<Card> cardList) {
        if (cardList.size() < 5) return false;
        cardList.sort(Comparator.reverseOrder());
        // 最大到A
        if (cardList.get(cardList.size() - 1).getLevel().getLevel() >= 13) return false;
        Card last = cardList.get(0);
        for (int i = 1; i < cardList.size(); ++i) {
            final Card now = cardList.get(i);
            if (now.getLevel().getLevel() - last.getLevel().getLevel() != 1) return false;
            last = now;
        }
        return true;
    }

    private static boolean isStraightPair(List<Card> cardList) {
        // 连对大于三对
        if (cardList.size() < 6 || cardList.size() % 2 != 0) return false;
        cardList.sort(Comparator.reverseOrder());
        // 最大到A
        if (cardList.get(cardList.size() - 1).getLevel().getLevel() >= 13) return false;
        int last = 0;
        for (int i = 0; i < cardList.size(); i += 2) {
            Card now1 = cardList.get(i);
            Card now2 = cardList.get(i + 1);
            if (now1.getLevel().getLevel() != now2.getLevel().getLevel()) return false;
            if (last != 0) {
                if (now1.getLevel().getLevel() - last != 1) return false;
            }
            last = now1.getLevel().getLevel();
        }
        return true;
    }

    private static boolean isJokerBomb(List<Card> cardList) {
        if (cardList.size() != 2) return false;
        final int sum = cardList.stream().mapToInt(card -> card.getLevel().getLevel()).sum();
        return sum == JOKER_BOMB_VALUE;
    }

    private static boolean isAircraft(List<Card> cardList) {
        final Map<Integer, List<Card>> cardGroup = getCardGroup(cardList);
        int last = 0;
        for (Map.Entry<Integer, List<Card>> entry : cardGroup.entrySet()) {
            if (entry.getValue().size() != 3) return false;
            if (last != 0) {
                if (entry.getKey() - last != 1) return false;
            }
            last = entry.getKey();
        }
        return true;
    }

    private static boolean isAircraftWithWing(List<Card> cardList) {
        final Map<Integer, List<Card>> cardGroup = getCardGroup(cardList);
        if (cardGroup.size() % 2 != 0) return false;
        Map<Integer, List<Card>> treeMap = new TreeMap<>();
        Map<Integer, List<Card>> otherMap = new TreeMap<>();
        int singleSize = 0;
        for (Map.Entry<Integer, List<Card>> entry : cardGroup.entrySet()) {
            final List<Card> value = entry.getValue();
            if (value.size() == 3) {
                treeMap.put(entry.getKey(), value);
            } else {
                if (value.size() > 2) return false;
                if (singleSize != 0) {
                    // 翅膀尺寸需相同
                    if (value.size() != singleSize) return false;
                }
                singleSize = value.size();
                otherMap.put(entry.getKey(), value);
            }
        }
        // 判断三对和三单/三双组数是否相同
        if (treeMap.size() != otherMap.size()) return false;
        // 判断三对是否相连
        int last = 0;
        for (int key : treeMap.keySet()) {
            if (last != 0) {
                if (key - last != 1) return false;
            }
            last = key;
        }
        return true;
    }

    /**
     * @param limitMin 必须有一个分组符合该数量
     * @param limitMax 必须另有一个分组符合该数量
     * @param groupCount 分组数
     * */
    private static boolean checkGroup(@Nullable Integer limitMin, @Nullable Integer limitMax, int groupCount, List<Card> cardList) {
        // level -> card
        final Map<Integer, List<Card>> cardGroup = getCardGroup(cardList);
        if (groupCount != cardGroup.size()) return false;
        int minGroupCount = 0;
        if (limitMin != null) ++ minGroupCount;
        if (limitMax != null) ++ minGroupCount;
        if (cardGroup.size() < minGroupCount) return false;
        final List<Integer> countList = cardGroup.values().stream().map(List::size).collect(Collectors.toList());
        if (limitMin != null) {
            if (!countList.contains(limitMin)) return false;
            countList.remove((Object) limitMin);
        }
        if (limitMax != null) {
            if (!countList.contains(limitMax)) return false;
            countList.remove((Object) limitMax);
        }
        return true;
    }

    /**
     * 以卡牌等级将卡牌分组
     * 分组等级 -> 卡牌列表
     * */
    public static Map<Integer, List<Card>> getCardGroup(List<Card> cardList) {
        Map<Integer, List<Card>> cardGroup = new TreeMap<>();
        for (Card card : cardList) {
            cardGroup.computeIfAbsent(card.getLevel().getLevel(), key -> new ArrayList<>()).add(card);
        }
        return cardGroup;
    }

    private static boolean isAllLevelEquals(List<Card> cardList) {
        if (cardList.size() <= 1) return true;
        final int level = cardList.get(0).getLevel().getLevel();
        for (int i = 1; i < cardList.size(); ++ i) {
            if (cardList.get(i).getLevel().getLevel() != level) {
                return false;
            }
        }
        return true;
    }
    
}
