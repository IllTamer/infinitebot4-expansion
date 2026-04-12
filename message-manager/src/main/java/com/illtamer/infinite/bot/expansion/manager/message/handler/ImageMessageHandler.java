package com.illtamer.infinite.bot.expansion.manager.message.handler;

import com.illtamer.infinite.bot.expansion.manager.message.config.ImageConfig;
import com.illtamer.infinite.bot.expansion.manager.message.config.MessageConfig;
import com.illtamer.infinite.bot.expansion.manager.message.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.manager.message.render.ImageRenderer;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionLogger;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 图片消息处理器（含纯图片和文字+图片组合消息）
 * 图片通过 Java2D 渲染后以 Base64 编码发送
 */
public class ImageMessageHandler implements MessageHandler {

    private final ImageRenderer renderer;
    private final ExpansionLogger logger;
    private final Language language;
    private final MessageResponseSender responseSender;

    public ImageMessageHandler(ImageRenderer renderer,
                               ExpansionLogger logger,
                               Language language,
                               MessageResponseSender responseSender) {
        this.renderer = renderer;
        this.logger = logger;
        this.language = language;
        this.responseSender = responseSender;
    }

    @Override
    public void handle(GroupMessageEvent event, MessageConfig config) {
        OfflinePlayer player = getPlayer(event);
        ImageConfig imageConfig = config.getImage();

        try {
            String base64 = renderer.render(imageConfig, player);
            if (base64 == null) {
                responseSender.sendText(event, config, language.get("image-render-error"));
                return;
            }

            // 发送图片（base64 格式）
            String imageUrl = "base64://" + base64;
            responseSender.sendMessage(event, config, MessageBuilder.json().image(imageUrl, null).build());

            // 组合消息：图片发送后再发文字
            if (config.getType() == MessageConfig.MessageType.COMBINED && !config.getText().isEmpty()) {
                List<String> lines = Placeholder.set(config.getText(), player);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.size(); i++) {
                    sb.append(lines.get(i));
                    if (i < lines.size() - 1) sb.append("\n");
                }
                responseSender.sendText(event, config, sb.toString());
            }
        } catch (IOException e) {
            logger.warn("[MessageManager] 图片渲染异常: " + e.getMessage());
            responseSender.sendText(event, config, language.get("image-render-error"));
        }
    }

    private OfflinePlayer getPlayer(GroupMessageEvent event) {
        var data = StaticAPI.getRepository().queryByUserId(event.getSender().getUserId());
        if (data == null || data.getPreferUUID() == null) return null;
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(data.getPreferUUID()));
        } catch (Exception e) {
            return null;
        }
    }

}
