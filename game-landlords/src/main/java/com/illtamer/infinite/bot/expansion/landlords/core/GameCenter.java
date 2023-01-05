package com.illtamer.infinite.bot.expansion.landlords.core;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GameCenter {

    // 游戏是否开始
    public static boolean start = false;
    // 出牌阶段
    public static boolean play = false;
    // 抢地主轮次
    public static int grabTurn = 0;
    // 地主牌
    public static List<Card> remain = new ArrayList<>(3);

    public static Pair<Participant, Pair<ActionType, List<Card>>> lastSend;

    /**
     * 开始游戏
     * 初始化三人手牌
     *
     * @param ps ps.length == 3
     */
    public static void startup(Collection<Participant> ps) {
        if (ps.size() != 3)
            throw new IllegalArgumentException("参与人数不为三人，实际: " + ps.size());
        start = true;
        final List<Card> cardList = construct();
        // 洗牌
        Collections.shuffle(cardList);
        final Iterator<Participant> iterator = ps.iterator();
        for (int i = 0; i < 3; ++ i) {
            final List<Card> list = new ArrayList<>(20);
            for (int j = 0; j < 17; ++ j) {
                list.add(cardList.get(j*3+i));
            }
            Collections.sort(list);
            iterator.next().setCardList(list);
        }
        final List<Card> remainCards = cardList.subList(51, 54);
        Collections.sort(remainCards);
        remain.addAll(remainCards);
    }

    /**
     * 检查能否打出指定牌
     *
     * @apiNote 未改变玩家牌组
     * @param cardStr 以 ' ' 隔开的 {@link LevelEnum#getName()}
     * @return 是否运行: 被打出的牌
     */
    @Nullable
    public static Pair<ActionType, List<Card>> canPlayHandCard(Participant player, String cardStr) {
        final String[] split = cardStr.split(" ");
        final List<Card> sendCards = new ArrayList<>(split.length);
        final List<Card> cardList = player.getCardList();
        final List<Card> tempCardList = new ArrayList<>(cardList);
        for (String s : split) {
            if (s.length() == 0) continue;
            final LevelEnum levelEnum = LevelEnum.parse(s);
            if (levelEnum == null) return null;
            boolean find = false;
            for (Card card : tempCardList) {
                if (card.getLevel() == levelEnum) {
                    sendCards.add(card);
                    tempCardList.remove(card);
                    find = true;
                    break;
                }
            }
            if (!find)
                return null;
        }
        final ActionType type = ActionChecker.getActionType(sendCards);
        if (type == null)
            return null;
        return new Pair<>(type, sendCards);
    }

    /**
     * 检查当前行动是否合理
     */
    public static boolean canTakeAction(ActionType action, List<Card> cardList, Participant player) {
        final boolean can = doCanTakeAction(action, cardList, player);
        lastSend = new Pair<>(player, new Pair<>(action, cardList));
        return can;
    }

    private static boolean doCanTakeAction(ActionType action, List<Card> cardList, Participant player) {
        if (lastSend == null) return true;
        if (lastSend.getKey().equals(player)) return true;
        final Pair<ActionType, List<Card>> pair = lastSend.getValue();
        final ActionType lastAction = pair.getKey();
        final List<Card> lastCardList = pair.getValue();
        if (action.getLevel() != lastAction.getLevel()) return action.getLevel() > lastAction.getLevel();
        // equal action level
        if (action != lastAction) return false;
        // same action
        cardList.sort(Comparator.reverseOrder());
        switch (action) {
            case SINGLE:
            case PAIR:
            case THREE:
            case BOMB:
            case STRAIGHT:
            case STRAIGHT_PAIR:
                return cardList.get(0).getLevel().getLevel() > lastCardList.get(0).getLevel().getLevel();
        }
        final Map<Integer, List<Card>> cardGroup = ActionChecker.getCardGroup(cardList);
        final Map<Integer, List<Card>> lastCardGroup = ActionChecker.getCardGroup(lastCardList);
        final Integer level;
        final Integer lastLevel;
        if (action == ActionType.FOUR_WITH_TWO) {
            level = cardGroup.entrySet().stream().filter(entry -> entry.getValue().size() == 4).findFirst().get().getKey();
            lastLevel = lastCardGroup.entrySet().stream().filter(entry -> entry.getValue().size() == 4).findFirst().get().getKey();
        } else {
            // THREE_WITH_ONE THREE_WITH_PAIR
            // AIRCRAFT AIRCRAFT_WITH_WINGS
            level = cardGroup.entrySet().stream().filter(entry -> entry.getValue().size() == 3).findFirst().get().getKey();
            lastLevel = lastCardGroup.entrySet().stream().filter(entry -> entry.getValue().size() == 3).findFirst().get().getKey();
        }
        return level > lastLevel;
    }

    // 54 = 13 * 4 + 2
    private static List<Card> construct() {
        List<Card> cardList = new ArrayList<>(54);
        for (int i = 0; i < 52; ++i) {
            final SuitEnum suit = SuitEnum.parse(i % 4);
            final LevelEnum level = LevelEnum.getLevelEnum(i % 13);
            cardList.add(new Card(i, suit, level));
        }
        cardList.add(new Card(52, null, LevelEnum._joker));
        cardList.add(new Card(53, null, LevelEnum._JOKER));
        return cardList;
    }

}
