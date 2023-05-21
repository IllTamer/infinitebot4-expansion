package com.illtamer.infinite.bot.expansion.chatgpt.driver;

import com.github.plexpt.chatgpt.Chatbot;
import com.github.plexpt.chatgpt.Config;
import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.expansion.chatgpt.Configuration;
import com.illtamer.infinite.bot.minecraft.expansion.automation.Registration;

import java.util.Map;

public class Davinci002Handler implements Handler {

    private final Chatbot chatbot;

    public Davinci002Handler() {
        this.chatbot = createDavinci2Instance();
    }

    private static Chatbot createDavinci2Instance() {
        final Configuration config = Registration.get(Configuration.class);
        final String email = config.getEmail();
        final String password = config.getPassword();
        if (email == null || password == null || email.length() == 0 || password.length() == 0) {
            final Pair<String, String> tokenAndCf = Handler.getTokenAndCf(config);
            return new Chatbot(tokenAndCf.getKey(), tokenAndCf.getValue(), config.getUserAgent());
        } else {
            Config aiConfig = new Config();
            aiConfig.setEmail(email);
            aiConfig.setPassword(password);
            return new Chatbot(aiConfig);
        }
    }

    @Override
    public Pair<String, String> createConversation(String message) {
        final Map<String, Object> response = chatbot.getChatResponse(message);
        return new Pair<>((String) response.get("message"), (String) response.get("conversation_id"));
    }

    @Override
    public String getResponse(String message, String conversationId) {
        chatbot.setConversationId(conversationId);
        final Map<String, Object> response = chatbot.getChatResponse(message);
        return (String) response.get("message");
    }

}
