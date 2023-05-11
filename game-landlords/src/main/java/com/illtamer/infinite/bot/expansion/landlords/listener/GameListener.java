package com.illtamer.infinite.bot.expansion.landlords.listener;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.PrivateMessageEvent;
import com.illtamer.infinite.bot.api.exception.APIInvokeException;
import com.illtamer.infinite.bot.api.handler.OpenAPIHandling;
import com.illtamer.infinite.bot.api.message.Message;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.expansion.landlords.config.Configuration;
import com.illtamer.infinite.bot.expansion.landlords.core.GameCenter;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.ActionType;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.Card;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.Participant;
import com.illtamer.infinite.bot.expansion.landlords.graphic.Brush;
import com.illtamer.infinite.bot.expansion.landlords.util.ImageUtil;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.automation.Registration;
import org.bukkit.Bukkit;

import java.util.*;

public class GameListener implements Listener {

    private final Configuration configuration;
    private final Map<Long, Participant> playerMap = new HashMap<>(3);
    // 玩家动作顺序队列
    private final Deque<Participant> playerQueue = new LinkedList<>();
    // 期望抢地主的玩家列表
    private final List<Participant> grabList = new LinkedList<>();

    public GameListener() {
        this.configuration = Registration.get(Configuration.class);
        init();
    }

    @EventHandler
    public void onPrivate(PrivateMessageEvent event) {
        if (!event.getMessage().isTextOnly()) return;
        final Long userId = event.getUserId();
        final String message = event.getRawMessage();
        if (configuration.getSee().equals(message)) {
            final Participant player = playerMap.get(userId);
            if (player == null || !GameCenter.start) {
                event.reply("您尚未加入游戏或游戏还未开始");
                return;
            }
            event.reply(createHandCardListMessage(player));
        }
    }

    @EventHandler
    public void onKeyword(GroupMessageEvent event) {
        if (!event.getMessage().isTextOnly() || !StaticAPI.inGroups(event.getGroupId())) return;
        final Long userId = event.getUserId();
        final String message = event.getRawMessage();
        if (StaticAPI.isAdmin(userId) && configuration.getStop().equals(message)) {
            stop("管理员中止了游戏！", event);
            return;
        }
        final boolean start = GameCenter.start;
        if (!start && configuration.getJoin().equals(message)) {
            handleJoinEvent(event);
            return;
        }
        if (!playerMap.containsKey(userId)) return;
        if (start && configuration.getGrab().equals(message)) {
            handleGrabEvent(true, event);
        } else if (start && configuration.getDisgrab().equals(message)) {
            handleGrabEvent(false, event);
        } else if (GameCenter.play && message.startsWith(configuration.getSend())) {
            handleSendEvent(true, event);
        } else if (GameCenter.play && message.startsWith(configuration.getDissend())) {
            handleSendEvent(false, event);
        }
    }

    private void handleSendEvent(boolean send, GroupMessageEvent event) {
        final Participant player = getTurnPlayer(event.getUserId());
        if (player == null) {
            event.reply("您未参加游戏或未到发言轮次");
            return;
        }
        if (!send) {
            turnNextPlayer();
            event.sendGroupMessage(MessageBuilder.json()
                    .at(playerQueue.getFirst().getUserId())
                    .text("上家跳过出牌，请出牌")
                    .build());
            return;
        }
        final String cardStr = event.getRawMessage().substring(configuration.getSend().length());
        final Pair<ActionType, List<Card>> pair = GameCenter.canPlayHandCard(playerQueue.getFirst(), cardStr);
        if (pair == null) {
            event.reply("出牌不符合规则，请重新出牌！");
            return;
        }
        final ActionType action = pair.getKey();
        final List<Card> cardList = pair.getValue();
        if (!GameCenter.canTakeAction(action, cardList, player)) {
            event.reply("出牌不符合规则，请重新出牌！");
            return;
        }
        // remove card list
        player.getCardList().removeIf(cardList::contains);
        // card list size check
        final int remain = player.getCardList().size();
        if (remain == 0) {
            stop(MessageBuilder.json()
                    .text("恭喜玩家(" + (player.isLandlord() ? "地主" : "农民") + ")")
                    .at(player.getUserId())
                    .text("获得胜利！", false)
                    .build(), event);
        } else {
            turnNextPlayer();
            event.reply(MessageBuilder.json()
                    .image(UUID.randomUUID().toString(), ImageUtil.imageToBase64(Brush.drawHandCardList(cardList)))
                    .text("出牌 [" + action.getName() + "] 成功，剩余 " + remain + " 张，请")
                    .at(playerQueue.getFirst().getUserId())
                    .text("出牌", false)
                    .build());
            OpenAPIHandling.sendMessage(createHandCardListMessage(player), player.getUserId());
        }
    }

    private void handleGrabEvent(boolean grab, GroupMessageEvent event) {
        // 二轮抢地主
        if (GameCenter.grabTurn == 4) {
            if (event.getUserId() != grabList.get(0).getUserId()) {
                event.reply("未到发言轮次");
                return;
            }
            if (grab || grabList.size() == 1) {
                // 剩余一个自动继承
                selectGrab(grabList.get(0), event);
                return;
            }
            if (grabList.size() == 2) {
                selectGrab(grabList.get(1), event);
            } else { // grabList.size() == 3
                grabList.remove(0);
                event.sendGroupMessage(MessageBuilder.json()
                        .text("是否再抢地主？")
                        .at(grabList.get(0).getUserId()).build());
            }
            return;
        }
        final Participant player = getTurnPlayer(event.getUserId());
        if (player == null) {
            event.reply("您未参加游戏或未到发言轮次");
            return;
        }
        ++ GameCenter.grabTurn;
        if (GameCenter.grabTurn <= 3) {
            if (grab) grabList.add(player);
            turnNextPlayer();
            if (GameCenter.grabTurn == 3) {
                if (grabList.size() == 0) {
                    stop("无人抢地主，游戏结束。", event);
                } else if (grabList.size() == 1) {
                    selectGrab(grabList.get(0), event);
                } else { // grabList.size() >= 2
                    ++ GameCenter.grabTurn;
                    event.sendGroupMessage(MessageBuilder.json()
                            .text("是否再抢地主？")
                            .at(grabList.get(0).getUserId()).build());
                }
            } else {
                event.sendGroupMessage(MessageBuilder.json()
                        .text("是否抢地主？")
                        .at(playerQueue.getFirst().getUserId()).build());
            }
        }
    }

    private void handleJoinEvent(GroupMessageEvent event) {
        if (GameCenter.start) {
            event.reply("游戏已开始，请耐心等待");
            return;
        }
        if (playerMap.size() == 3) {
            event.reply("游戏人数已满");
            return;
        }
        final Participant player = new Participant();
        player.setUserId(event.getUserId());
        playerMap.put(event.getUserId(), player);
        event.reply("加入成功！房间内等待玩家数: " + playerMap.size() + "\n请确保已添加机器人为好友！");
        if (playerMap.size() == 3) {
            final MessageBuilder startBuilder = MessageBuilder.json();
            playerMap.keySet().forEach(startBuilder::at);
            startBuilder.text("游戏即将开始，发牌中，请留意私信...");
            event.sendGroupMessage(startBuilder.build());
            // start
            GameCenter.startup(playerMap.values());
            for (Map.Entry<Long, Participant> entry : playerMap.entrySet()) {
                try {
                    OpenAPIHandling.sendMessage(createHandCardListMessage(entry.getValue()), entry.getKey());
                } catch (APIInvokeException e) {
                    if ("SEND_MSG_API_ERROR".equals(e.getMessage())) {
                        stop("玩家 " + entry.getKey() + " 未添加为机器人好友", event);
                    } else {
                        e.printStackTrace();
                    }
                    return;
                }
            }
            playerQueue.addAll(playerMap.values());
            rearrangeQueue();
            final MessageBuilder indexBuilder = MessageBuilder.json().text("手牌发放完毕，发言顺序依次为：");
            for (Participant p : playerQueue) {
                indexBuilder.at(p.getUserId());
            }
            indexBuilder.text("是否抢地主？").at(playerQueue.peek().getUserId());
            event.sendGroupMessage(indexBuilder.build());
        }
    }

    // 选定地主
    private void selectGrab(Participant grabPlayer, GroupMessageEvent event) {
        playerQueue.remove(grabPlayer);
        playerQueue.addFirst(grabPlayer);
        final long userId = grabPlayer.getUserId();
        grabPlayer.setLandlord(true);
        event.sendGroupMessage(MessageBuilder.json()
                .at(userId)
                .text("成为地主，地主牌为：")
                .image(UUID.randomUUID().toString(), ImageUtil.imageToBase64(Brush.drawTotalCardList(GameCenter.remain)))
                .text("请地主出牌")
                .build());
        final List<Card> cardList = grabPlayer.getCardList();
        cardList.addAll(GameCenter.remain);
        Collections.sort(cardList);
        GameCenter.play = true;
        Bukkit.getScheduler().runTaskAsynchronously(Bootstrap.getInstance(), () -> OpenAPIHandling.sendMessage(createHandCardListMessage(grabPlayer), userId));
    }

    private Message createHandCardListMessage(Participant p) {
        final List<Card> cardList = p.getCardList();
        if (cardList.size() == 0) return MessageBuilder.json().text("您的手牌已空").build();
        return MessageBuilder.json()
                .text("您当前的手牌为")
                .image(UUID.randomUUID().toString(), ImageUtil.imageToBase64(Brush.drawHandCardList(cardList)))
                .build();
    }

    /**
     * 获取处于当前发言轮次的玩家对象
     * */
    private Participant getTurnPlayer(long userId) {
        final Participant player = playerMap.get(userId);
        if (player == null) return null;
        return player.equals(playerQueue.peek()) ? player : null;
    }

    private void turnNextPlayer() {
        final Participant poll = playerQueue.poll();
        playerQueue.add(poll);
    }

    private void stop(String reason, GroupMessageEvent event) {
        event.sendGroupMessage(reason);
        init();
    }

    private void stop(Message reason, GroupMessageEvent event) {
        event.sendGroupMessage(reason);
        init();
    }

    private void init() {
        playerMap.clear();
        playerQueue.clear();
        grabList.clear();
        GameCenter.start = false;
        GameCenter.play = false;
        GameCenter.grabTurn = 0;
        GameCenter.remain.clear();
        GameCenter.lastSend = null;
    }

    private void rearrangeQueue() {
        final int time = new Random().nextInt(3);
        for (int i = 0; i < time; ++ i) {
            final Participant poll = playerQueue.poll();
            playerQueue.add(poll);
        }
    }

}
