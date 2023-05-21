package com.illtamer.infinite.bot.expansion.landlords.graphic;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.landlords.LandLordsGame;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.Card;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.LevelEnum;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.SuitEnum;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Brush {

    private static final int DEVISE = 2;
    private static final int CARD_HEIGHT = 159;
    private static final int CARD_PART_WIDTH = 43;
    private static final int CARD_TOTAL_WIDTH = 116;

    // Pair -> x: y
    private static final Map<SuitEnum, Map<LevelEnum, Pair<Integer, Integer>>> CARD_POSITION_MAP;
    private static final Pair<Integer, Integer> JOKER_POSITION = new Pair<>(5, 0);
    private static final Map<SuitEnum, Map<LevelEnum, int[]>> CARD_IMAGE_MAP;
    private static final Map<SuitEnum, Map<LevelEnum, int[]>> CARD_PART_IMAGE_MAP;
    private static final int[][] JOKER_IMAGE;
    private static final int[][] JOKER_PART_IMAGE;

    private static BufferedImage cardImage;

    /**
     * 绘制玩家手牌
     * */
    public static BufferedImage drawHandCardList(@NotNull List<Card> cardList) {
        Assert.notEmpty(cardList, "Empty card list!");
        final int size = cardList.size();
        BufferedImage handCardImage;
        if (size == 1) {
            final int[] cardRGB = getCardRGB(cardList.get(0), false);
            handCardImage = new BufferedImage(CARD_TOTAL_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
            handCardImage.setRGB(0, 0, CARD_TOTAL_WIDTH, CARD_HEIGHT, cardRGB, 0, CARD_TOTAL_WIDTH);
            return handCardImage;
        }
        handCardImage = new BufferedImage((size-1)*CARD_PART_WIDTH + CARD_TOTAL_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < size-1; ++ i) {
            final int[] cardRGB = getCardRGB(cardList.get(i), true);
            handCardImage.setRGB(i*CARD_PART_WIDTH, 0, CARD_PART_WIDTH, CARD_HEIGHT, cardRGB, 0, CARD_PART_WIDTH);
        }
        final int[] lastRGB = getCardRGB(cardList.get(size - 1), false);
        handCardImage.setRGB((size-1)*CARD_PART_WIDTH, 0, CARD_TOTAL_WIDTH, CARD_HEIGHT, lastRGB, 0, CARD_TOTAL_WIDTH);
        return handCardImage;
    }

    /**
     * 绘制完整牌面
     * */
    public static BufferedImage drawTotalCardList(@NotNull List<Card> cardList) {
        Assert.notEmpty(cardList, "Empty card list!");
        final int size = cardList.size();
        BufferedImage totalCard = new BufferedImage(size*CARD_TOTAL_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int i = size-1; i >= 0; -- i) {
            final int[] cardRGB = getCardRGB(cardList.get(i), false);
            totalCard.setRGB((size-i-1) * CARD_TOTAL_WIDTH, 0, CARD_TOTAL_WIDTH, CARD_HEIGHT, cardRGB, 0, CARD_TOTAL_WIDTH);
        }
        return totalCard;
    }

    private static int[] getCardRGB(Card card, boolean part) {
        final SuitEnum suit = card.getSuit();
        final LevelEnum level = card.getLevel();
        if (suit == null) {
            if (part)
                return level == LevelEnum._joker ? JOKER_PART_IMAGE[0] : JOKER_PART_IMAGE[1];
            else
                return level == LevelEnum._joker ? JOKER_IMAGE[0] : JOKER_IMAGE[1];
        } else {
            return part ? CARD_PART_IMAGE_MAP.get(suit).get(level) : CARD_IMAGE_MAP.get(suit).get(level);
        }
    }

    private static void initCardImageMap() {
        Assert.notNull(cardImage, "未能找到卡片资源，初始化失败");
        for (Map.Entry<SuitEnum, Map<LevelEnum, Pair<Integer, Integer>>> entry : CARD_POSITION_MAP.entrySet()) {
            final SuitEnum suitEnum = entry.getKey();
            final Map<LevelEnum, int[]> imageMap = CARD_IMAGE_MAP.computeIfAbsent(suitEnum, key -> new HashMap<>());
            final Map<LevelEnum, int[]> partImageMap = CARD_PART_IMAGE_MAP.computeIfAbsent(suitEnum, key -> new HashMap<>());
            for (Map.Entry<LevelEnum, Pair<Integer, Integer>> pairEntry : entry.getValue().entrySet()) {
                final LevelEnum levelEnum = pairEntry.getKey();
                final Pair<Integer, Integer> pair = pairEntry.getValue();
                final BufferedImage image = cardImage.getSubimage(pair.getKey() * CARD_TOTAL_WIDTH + (pair.getKey() + 1) * DEVISE, pair.getValue() * CARD_HEIGHT + (pair.getValue() + 1) * DEVISE, CARD_TOTAL_WIDTH, CARD_HEIGHT);
                final BufferedImage partImage = cardImage.getSubimage(pair.getKey() * CARD_TOTAL_WIDTH + (pair.getKey() + 1) * DEVISE, pair.getValue() * CARD_HEIGHT + (pair.getValue() + 1) * DEVISE, CARD_PART_WIDTH, CARD_HEIGHT);
//                try {
//                    ImageIO.write(partImage, "png", new File("C:\\Users\\Bacon\\Desktop\\png\\" + suitEnum.getIndex() + '-' + levelEnum.getIndex() + ".png"));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                imageMap.put(levelEnum, image.getRGB(0, 0, CARD_TOTAL_WIDTH, CARD_HEIGHT, null, 0, CARD_TOTAL_WIDTH));
                partImageMap.put(levelEnum, partImage.getRGB(0, 0, CARD_PART_WIDTH, CARD_HEIGHT, null, 0, CARD_PART_WIDTH));
            }
        }
        JOKER_IMAGE[0] = cardImage.getSubimage(JOKER_POSITION.getKey() * CARD_TOTAL_WIDTH + (JOKER_POSITION.getKey() + 1) * DEVISE, JOKER_POSITION.getValue() * CARD_HEIGHT + (JOKER_POSITION.getValue() + 1) * DEVISE, CARD_TOTAL_WIDTH, CARD_HEIGHT)
                .getRGB(0, 0, CARD_TOTAL_WIDTH, CARD_HEIGHT, null, 0, CARD_TOTAL_WIDTH);
        JOKER_PART_IMAGE[0] = cardImage.getSubimage(JOKER_POSITION.getKey() * CARD_TOTAL_WIDTH + (JOKER_POSITION.getKey() + 1) * DEVISE, JOKER_POSITION.getValue() * CARD_HEIGHT + (JOKER_POSITION.getValue() + 1) * DEVISE, CARD_PART_WIDTH, CARD_HEIGHT)
                .getRGB(0, 0, CARD_PART_WIDTH, CARD_HEIGHT, null, 0, CARD_PART_WIDTH);
        JOKER_IMAGE[1] = cardImage.getSubimage((JOKER_POSITION.getKey()+1) * CARD_TOTAL_WIDTH + (JOKER_POSITION.getKey() + 2) * DEVISE, JOKER_POSITION.getValue() * CARD_HEIGHT + (JOKER_POSITION.getValue() + 1) * DEVISE, CARD_TOTAL_WIDTH, CARD_HEIGHT)
                .getRGB(0, 0, CARD_TOTAL_WIDTH, CARD_HEIGHT, null, 0, CARD_TOTAL_WIDTH);
        JOKER_PART_IMAGE[1] = cardImage.getSubimage((JOKER_POSITION.getKey()+1) * CARD_TOTAL_WIDTH + (JOKER_POSITION.getKey() + 2) * DEVISE, JOKER_POSITION.getValue() * CARD_HEIGHT + (JOKER_POSITION.getValue() + 1) * DEVISE, CARD_PART_WIDTH, CARD_HEIGHT)
                .getRGB(0, 0, CARD_PART_WIDTH, CARD_HEIGHT, null, 0, CARD_PART_WIDTH);
    }

    static {
        try {
            cardImage = ImageIO.read(LandLordsGame.getInstance().getResource("card.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        CARD_POSITION_MAP = new HashMap<>();
        Map<LevelEnum, Pair<Integer, Integer>> heartMap = new HashMap<>();
        heartMap.put(LevelEnum._K, new Pair<>(9, 1));
        heartMap.put(LevelEnum._Q, new Pair<>(10, 1));
        heartMap.put(LevelEnum._J, new Pair<>(11, 1));
        heartMap.put(LevelEnum._10, new Pair<>(12, 1));
        heartMap.put(LevelEnum._9, new Pair<>(0, 2));
        heartMap.put(LevelEnum._8, new Pair<>(1, 2));
        heartMap.put(LevelEnum._7, new Pair<>(2, 2));
        heartMap.put(LevelEnum._6, new Pair<>(3, 2));
        heartMap.put(LevelEnum._5, new Pair<>(4, 2));
        heartMap.put(LevelEnum._4, new Pair<>(5, 2));
        heartMap.put(LevelEnum._3, new Pair<>(7, 2));
        heartMap.put(LevelEnum._2, new Pair<>(8, 2));
        heartMap.put(LevelEnum._A, new Pair<>(9, 2));
        CARD_POSITION_MAP.put(SuitEnum.HEART, heartMap);
        Map<LevelEnum, Pair<Integer, Integer>> spadeMap = new HashMap<>();
        spadeMap.put(LevelEnum._K, new Pair<>(7, 0));
        spadeMap.put(LevelEnum._Q, new Pair<>(8, 0));
        spadeMap.put(LevelEnum._J, new Pair<>(9, 0));
        spadeMap.put(LevelEnum._10, new Pair<>(11, 0));
        spadeMap.put(LevelEnum._9, new Pair<>(12, 0));
        spadeMap.put(LevelEnum._8, new Pair<>(0, 1));
        spadeMap.put(LevelEnum._7, new Pair<>(1, 1));
        spadeMap.put(LevelEnum._6, new Pair<>(2, 1));
        spadeMap.put(LevelEnum._5, new Pair<>(3, 1));
        spadeMap.put(LevelEnum._4, new Pair<>(4, 1));
        spadeMap.put(LevelEnum._3, new Pair<>(5, 1));
        spadeMap.put(LevelEnum._2, new Pair<>(6, 1));
        spadeMap.put(LevelEnum._A, new Pair<>(7, 1));
        CARD_POSITION_MAP.put(SuitEnum.SPADE, spadeMap);
        Map<LevelEnum, Pair<Integer, Integer>> diamond = new HashMap<>();
        diamond.put(LevelEnum._K, new Pair<>(11, 3));
        diamond.put(LevelEnum._Q, new Pair<>(12, 3));
        diamond.put(LevelEnum._J, new Pair<>(0, 4));
        diamond.put(LevelEnum._10, new Pair<>(1, 4));
        diamond.put(LevelEnum._9, new Pair<>(0, 0));
        diamond.put(LevelEnum._8, new Pair<>(1, 0));
        diamond.put(LevelEnum._7, new Pair<>(2, 0));
        diamond.put(LevelEnum._6, new Pair<>(3, 0));
        diamond.put(LevelEnum._5, new Pair<>(10, 0));
        diamond.put(LevelEnum._4, new Pair<>(8, 1));
        diamond.put(LevelEnum._3, new Pair<>(6, 2));
        diamond.put(LevelEnum._2, new Pair<>(4, 3));
        diamond.put(LevelEnum._A, new Pair<>(2, 4));
        CARD_POSITION_MAP.put(SuitEnum.DIAMOND, diamond);
        Map<LevelEnum, Pair<Integer, Integer>> club = new HashMap<>();
        club.put(LevelEnum._K, new Pair<>(10, 2));
        club.put(LevelEnum._Q, new Pair<>(11, 2));
        club.put(LevelEnum._J, new Pair<>(12, 2));
        club.put(LevelEnum._10, new Pair<>(0, 3));
        club.put(LevelEnum._9, new Pair<>(1, 3));
        club.put(LevelEnum._8, new Pair<>(2, 3));
        club.put(LevelEnum._7, new Pair<>(3, 3));
        club.put(LevelEnum._6, new Pair<>(5, 3));
        club.put(LevelEnum._5, new Pair<>(6, 3));
        club.put(LevelEnum._4, new Pair<>(7, 3));
        club.put(LevelEnum._3, new Pair<>(8, 3));
        club.put(LevelEnum._2, new Pair<>(9, 3));
        club.put(LevelEnum._A, new Pair<>(10, 3));
        CARD_POSITION_MAP.put(SuitEnum.CLUB, club);

        CARD_IMAGE_MAP = new HashMap<>();
        CARD_PART_IMAGE_MAP = new HashMap<>();
        JOKER_IMAGE = new int[2][];
        JOKER_PART_IMAGE = new int[2][];
        initCardImageMap();
    }

}
