package com.illtamer.infinite.bot.expansion.message.message;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.message.Message;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageEntity;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageNode;

import java.util.HashMap;
import java.util.function.Function;

public class ResponseHandler {

    private static final HashMap<String, Function<Pair<MessageBuilder, MessageEvent>, MessageBuilder>> FUNCTION_MAP;

    public static void handle(MessageEntity entity, MessageEvent event) {
        System.out.println(entity);
        if (entity.getShowType() == MessageNode.ShowType.TEXT) {
            text(entity, event);
        } else if (entity.getShowType() == MessageNode.ShowType.IMAGE) {
            image(entity, event);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static void text(MessageEntity entity, MessageEvent event) {
        final MessageBuilder builder = MessageBuilder.json();
        Pair<MessageBuilder, MessageEvent> pair = new Pair<>(builder, event);
        for (String line : entity.getContent()) {
            final Function<Pair<MessageBuilder, MessageEvent>, MessageBuilder> function = FUNCTION_MAP.get(line);
            if (function != null) {
                function.apply(pair);
            } else {
                builder.text(line);
            }
        }
        final Message message = builder.build();
        if (event instanceof GroupMessageEvent) {
            ((GroupMessageEvent) event).reply(message, false);
        } else {
            event.reply(message);
        }
    }

    public static void image(MessageEntity entity, MessageEvent event) {
        // TODO
    }

    static {
        FUNCTION_MAP = new HashMap<>();
        FUNCTION_MAP.put("#sender", pair -> pair.getKey().at(pair.getValue().getUserId()));
        FUNCTION_MAP.put("#reply", pair -> pair.getKey().reply(pair.getValue().getMessageId()));
    }

}
