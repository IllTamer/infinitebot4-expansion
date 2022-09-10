package com.illtamer.infinite.bot.expansion.message.message;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.event.message.PrivateMessageEvent;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageNode;
import com.illtamer.infinite.bot.expansion.message.pojo.Trigger;
import com.illtamer.infinite.bot.expansion.message.util.LambdaFilter;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.repository.PlayerDataRepository;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MessageTrigger {

    private final PlayerDataRepository repository = StaticAPI.getRepository();
    private final List<Pair<Trigger, Pair<MessageNode, Object>>> pairList;

    public MessageTrigger(List<Pair<MessageNode, Object>> messageNodes) {
        this.pairList = messageNodes.stream()
                .map(pair -> new Pair<>(pair.getKey().getTrigger(), pair))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有可触发的自制消息节点
     * */
    public List<Pair<MessageNode, Object>> select(MessageEvent event) {
        return pairList.stream()
                .filter(pair -> doFilter(event, pair.getKey()))
                .map(Pair::getValue)
                .collect(Collectors.toList());
    }

    protected boolean doFilter(MessageEvent event, Trigger trigger) {
        Filter filter = new Filter(event, trigger);
        return LambdaFilter.of(filter)
                .is(Filter::checkSource)
                .is(Filter::checkAdmin)
                .is(Filter::checkKeys)
                .is(Filter::checkBind) // db operate latest
                .result();
    }

    private class Filter {
        private final MessageEvent event;
        private final Trigger trigger;

        private Filter(MessageEvent event, Trigger trigger) {
            this.event = event;
            this.trigger = trigger;
        }

        private boolean checkSource() {
            switch (trigger.getSource()) {
                case GROUP: return event instanceof GroupMessageEvent;
                case PRIVATE: return event instanceof PrivateMessageEvent;
                case ALL: {

                    return true;
                }
            }
            throw new IllegalArgumentException();
        }

        private boolean checkAdmin() {
            return !trigger.getAdmin() || StaticAPI.isAdmin(event.getUserId());
        }

        private boolean checkBind() {
            if (!trigger.getBind()) {
                return true;
            }
            return repository.queryByUserId(event.getUserId()) != null;
        }

        private boolean checkKeys() {
            final List<String> keys = trigger.getKeys();
            // 为空时任意消息皆可触发
            if (keys.size() == 0) return true;
            StringBuilder builder = new StringBuilder();
            event.getMessage().getCleanMessage().forEach(builder::append);
            String cleanMessage = builder.toString();
            if (cleanMessage.length() == 0) return false;
            switch (trigger.getType()) {
                case CONTAINS: {
                    return strListCheck(cleanMessage::contains, keys);
                }
                case EQUAL: {
                    return strListCheck(cleanMessage::equals, keys);
                }
            }
            throw new IllegalArgumentException();
        }

        private boolean strListCheck(Function<String, Boolean> function, List<String> keys) {
            final boolean all = trigger.getFilter();
            for (String key : keys) {
                final boolean contains = function.apply(key);
                if (all && !contains) return false;
                if (!all && contains) return true;
            }
            // 所有check都通过 || 没有一个check通过
            return all;
        }

    }

}
