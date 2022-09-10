package com.illtamer.infinite.bot.expansion.message.message;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.api.util.HttpRequestUtil;
import com.illtamer.infinite.bot.expansion.message.MessageManager;
import com.illtamer.infinite.bot.expansion.message.entity.SubmitSender;
import com.illtamer.infinite.bot.expansion.message.pojo.Api;
import com.illtamer.infinite.bot.expansion.message.pojo.Command;
import com.illtamer.infinite.bot.expansion.message.pojo.Image.InsertImage;
import com.illtamer.infinite.bot.expansion.message.pojo.Image.Setting;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageEntity;
import com.illtamer.infinite.bot.expansion.message.util.ImageUtil;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Function;

public class ResponseHandler {

    private static final Logger LOG = MessageManager.getInstance().getLogger();
    private static final HashMap<String, Function<Pair<MessageBuilder, MessageEvent>, MessageBuilder>> FUNCTION_MAP;

    private static Language language;

    public static void init(Language _language) {
        language = _language;
    }

    public static void handle(MessageEntity entity, MessageEvent event, OfflinePlayer player) {
        switch (entity.getShowType()) {
            case TEXT:
                text(entity, event, null);
                break;
            case IMAGE:
                image(entity, event);
                break;
            case API:
                api(entity, event);
                break;
            case COMMAND:
                command(entity, event, player);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static void text(MessageEntity entity, MessageEvent event, Map<String, Function<Pair<MessageBuilder, MessageEvent>, MessageBuilder>> externFunctionMap) {
        final HashMap<String, Function<Pair<MessageBuilder, MessageEvent>, MessageBuilder>> functionHashMap;
        if (externFunctionMap != null && externFunctionMap.size() != 0) {
            functionHashMap = new HashMap<>(FUNCTION_MAP);
            functionHashMap.putAll(externFunctionMap);
        } else {
            functionHashMap = FUNCTION_MAP;
        }

        final MessageBuilder builder = MessageBuilder.json();
        Pair<MessageBuilder, MessageEvent> pair = new Pair<>(builder, event);
        for (String line : entity.getContent()) {
            final Function<Pair<MessageBuilder, MessageEvent>, MessageBuilder> function = functionHashMap.get(line);
            if (function != null) {
                function.apply(pair);
            } else {
                builder.text(line.replace("\\n", "\n"), false);
            }
        }

        if (event instanceof GroupMessageEvent) {
            ((GroupMessageEvent) event).sendGroupMessage(builder.build());
        } else {
            event.reply(builder.build());
        }
    }

    public static void image(MessageEntity entity, MessageEvent event) {
        final List<String> contents = entity.getContent();
        final List<String> functionAble = new LinkedList<>();
        final List<String> lines = new ArrayList<>(contents.size());
        for (String content : contents) {
            if (content.startsWith("#")) {
                functionAble.add(content);
            } else {
                lines.add(content);
            }
        }

        com.illtamer.infinite.bot.expansion.message.pojo.Image image = (com.illtamer.infinite.bot.expansion.message.pojo.Image) entity.getAttribute();
        final Setting setting = image.getSetting();
        final List<InsertImage> insertImages = image.getInsertImages();
        final String imageStr = ImageUtil.draw(graphics -> {
                    for (InsertImage insertImage : insertImages) {
                        try {
                            Image drawImage = ImageIO.read(insertImage.getSource().get());
                            if (insertImage.getWidth() != 0 && insertImage.getHeight() != 0) {
                                drawImage   .getScaledInstance(insertImage.getWidth(), insertImage.getHeight(), Image.SCALE_DEFAULT);
                            }
                            graphics.drawImage(drawImage, insertImage.getStartX(), insertImage.getStartY(), null);
                        } catch (IOException e) {
                            LOG.warn("Exception occurred with MessageEntity:\n" + entity);
                            e.printStackTrace();
                        }
                    }
                },
                setting.getSource(),
                setting.getWidth(),
                setting.getHeight(),
                setting.getStartX(),
                setting.getStartY(),
                setting.getSpace(),
                setting.getColor(),
                image.getFont(),
                lines
        );
        if (imageStr == null) {
            event.reply("图片资源加载失败，请检查控制台输出");
            return;
        }
        functionAble.add("#image");
        entity.setContent(functionAble);
        text(entity, event, Collections.singletonMap("#image", pair -> pair.getKey().image("message-manager.png", imageStr)));
    }

    public static void api(MessageEntity entity, MessageEvent event) {
        final String url = ((Api) entity.getAttribute()).getUrl();
        final Pair<Integer, String> json = HttpRequestUtil.getJson(url, null);
        if (json.getKey() != 200) {
            event.reply(language.get("response", "web-api").replace("%code%", String.valueOf(json.getKey())));
            return;
        }
        text(entity, event, Collections.singletonMap("#content", pair -> pair.getKey().text(json.getValue(), false)));
    }

    public static void command(MessageEntity entity, MessageEvent event, OfflinePlayer player) {
        final Command command = (Command) entity.getAttribute();
        final boolean setOp = command.isOp();
        if (command.getType() == Command.Type.CONSOLE) {
            SubmitSender sender = new SubmitSender(Bukkit.getServer(), event, setOp, "message-manager#command-sender");
            for (String content : entity.getContent()) {
                Bukkit.getScheduler().runTask(Bootstrap.getInstance(), () ->
                        Bukkit.dispatchCommand(sender, content));
            }
        } else if (command.getType() == Command.Type.SELF) {
            final Player onlinePlayer = player.getPlayer();
            if (!player.isOnline() || onlinePlayer == null) {
                event.reply(language.get("response", "offline"));
                return;
            }
            final boolean playerOp = onlinePlayer.isOp();
            if (!playerOp && setOp) onlinePlayer.setOp(true);
            for (String content : entity.getContent()) {
                onlinePlayer.performCommand(content);
            }
            onlinePlayer.setOp(playerOp);
        } else throw new IllegalArgumentException();
    }

    static {
        FUNCTION_MAP = new HashMap<>();
        FUNCTION_MAP.put("#sender", pair -> pair.getKey().at(pair.getValue().getUserId()));
        FUNCTION_MAP.put("#reply", pair -> pair.getKey().reply(pair.getValue().getMessageId()));
    }

}
