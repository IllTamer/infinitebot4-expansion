package com.illtamer.infinite.bot.expansion.chatgpt.driver;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.util.HttpRequestUtil;
import com.illtamer.infinite.bot.expansion.chatgpt.Configuration;

public interface Handler {

    /**
     * @return response, sessionId
     * */
    Pair<String, String> createConversation(String message);

    String getResponse(String message, String conversationId);

    static Pair<String, String> getTokenAndCf(Configuration config) {
        return new Pair<>(getStaticUrl(config.getToken()), getStaticUrl(config.getCfClearance()));
    }

    static String getStaticUrl(String url) {
        if (url.startsWith("http")) {
            final Pair<Integer, String> pair = HttpRequestUtil.getJson(url, null);
            if (pair.getKey() != 200) {
                throw new IllegalArgumentException("token 资源不可用，state: " + pair.getKey());
            }
            return pair.getValue();
        }
        return url;
    }

}
