package com.illtamer.infinite.bot.expansion.chat.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息过滤器
 * */
public interface MessageFilter {

    Map<String, MessageFilter> MAP = new HashMap<>();

    /**
     * 初始化过滤器
     * */
    void init(List<String> keys);

    /**
     * 批量过滤
     * */
    List<String> doFilter(List<String> messages);

    /**
     * 单次过滤并返回结果
     * */
    boolean result(String message);

    /**
     * 过滤规则是否为空
     * */
    boolean isEmpty();

    /**
     * 是否拒绝纯特殊消息
     * */
    boolean rejectNoText();

}
