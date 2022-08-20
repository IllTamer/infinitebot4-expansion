package com.illtamer.infinite.bot.expansion.chat.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Filter {

    Map<String, Filter> MAP = new HashMap<>();

    void init(List<String> keys);

    List<String> doFilter(List<String> messages);

    boolean result(String message);

}
