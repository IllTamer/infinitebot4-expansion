package com.illtamer.infinite.bot.expansion.chat.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DenyContainsFilter implements MessageFilter {

    private final Set<String> keys = new HashSet<>();
    private boolean empty;

    @Override
    public void init(List<String> keys) {
        this.keys.addAll(keys);
        this.empty = keys.size() == 0;
    }

    @Override
    public List<String> doFilter(List<String> messages) {
        if (empty) return messages;
        return messages.stream()
                .filter(this::result)
                .collect(Collectors.toList());
    }

    @Override
    public boolean result(String message) {
        if (empty) return true;
        for (String key : keys) {
            if (message.contains(key)) return false;
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }

    @Override
    public boolean rejectNoText() {
        return false;
    }

}
