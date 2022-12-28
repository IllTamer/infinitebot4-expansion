package com.illtamer.infinite.bot.expansion.chatgpt.driver;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.expansion.chatgpt.Configuration;
import com.illtamer.infinite.bot.minecraft.expansion.automation.Registration;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class Davinci003Handler implements Handler {

    private final String model;
    private final Supplier<OpenAiService> serviceSupplier;
    private final Map<String, OpenAiService> serviceMap;

    public Davinci003Handler() {
        final Configuration config = Registration.get(Configuration.class);
        this.model = config.getModel().getName();
        final String apiToken = Handler.getStaticUrl(config.getToken());
        this.serviceSupplier = () -> new OpenAiService(apiToken);
        this.serviceMap = new HashMap<>();
    }

    @Override
    public Pair<String, String> createConversation(String message) {
        final UUID uuid = UUID.randomUUID();
        final OpenAiService service = serviceMap.computeIfAbsent(uuid.toString(), key -> serviceSupplier.get());
        final String text = doCreateConversation(message, service);
        return new Pair<>(text, uuid.toString());
    }

    @Override
    public String getResponse(String message, String conversationId) {
        OpenAiService service = serviceMap.get(conversationId);
        if (service == null) {
            service = serviceMap.computeIfAbsent(conversationId, key -> serviceSupplier.get());
        }
        return doCreateConversation(message, service);
    }

    private String doCreateConversation(String message, OpenAiService service) {
        final CompletionRequest request = new CompletionRequest();
        request.setModel(model);
        request.setPrompt(message);
        request.setTemperature(0.7);
        request.setMaxTokens(512);
        request.setTopP(1D);
        request.setBestOf(1);
        String text = service.createCompletion(request).getChoices().get(0).getText();
        while (text.startsWith("\n")) {
            text = text.substring(2);
        }
        return text;
    }

}
