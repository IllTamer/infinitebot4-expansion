package com.illtamer.infinite.bot.expansion.message.message;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.message.MessageManager;
import com.illtamer.infinite.bot.expansion.message.pojo.Image;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageNode;
import com.illtamer.infinite.bot.expansion.message.pojo.Trigger;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.util.ExpansionUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MessageLoader {

    // 自定义静态变量Map
    public static final Map<String, String> CUSTOM_PLACEHOLDERS = new HashMap<>();
    public static final List<Pair<MessageNode, Object>> MESSAGE_NODES = new ArrayList<>();

    private static final Logger LOGGER = MessageManager.getInstance().getLogger();

    public static void init(FileConfiguration configuration, File dataFolder, IExpansion expansion) {
        if (CUSTOM_PLACEHOLDERS.size() != 0) CUSTOM_PLACEHOLDERS.clear();
        if (MESSAGE_NODES.size() != 0) MESSAGE_NODES.clear();
        Optional.ofNullable(configuration.getConfigurationSection("custom-placeholder"))
                .orElseThrow(() -> new IllegalArgumentException("custom-placeholder节点不存在！"))
                .getValues(false).forEach((k, v) -> CUSTOM_PLACEHOLDERS.put(k, String.valueOf(v)));
        final String folderName = configuration.getString("folder", "/message");
        File folder = new File(dataFolder, folderName);
        LOGGER.info("加载了 " + CUSTOM_PLACEHOLDERS.size() + " 个自定义静态变量");
        if (!folder.exists()) {
            LOGGER.warn("消息节点资源文件夹不存在，默认目录与实例配置创建在: " + folder.getAbsolutePath());
            folder.mkdirs();
            InputStream input = expansion.getResource("message/examples.yml");
            ExpansionUtil.savePluginResource(folderName + "/examples.yml", false, dataFolder, input);
        }
        final File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            LOGGER.info("未找到可用的消息节点");
            return;
        }
        for (File file : files) {
            final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            MESSAGE_NODES.addAll(yaml.getKeys(false).stream()
                    .map(key -> deserialize(Optional.ofNullable(yaml.getConfigurationSection(key))
                            .orElseThrow(() -> new IllegalArgumentException("文件 " + file.getName() + " 中节点 " + key + " 解析失败"))))
                    .collect(Collectors.toList()));
        }
        LOGGER.info("加载了 " + MESSAGE_NODES.size() + " 个消息节点");
    }

    private static Pair<MessageNode, Object> deserialize(ConfigurationSection section) {
        MessageNode node = new MessageNode();
        node.setTrigger(deserializeTrigger(section.getConfigurationSection("trigger")));
        node.setPapi(section.getBoolean("papi"));
        node.setShowType(MessageNode.ShowType.parse(section.getString("show-type")));
        node.setContent(section.getStringList("content"));
        Image image = node.getShowType() == MessageNode.ShowType.IMAGE ?
                deserializeImage(section.getConfigurationSection(MessageNode.ShowType.IMAGE.getValue())) : null;
        return new Pair<>(node, image);
    }

    private static Trigger deserializeTrigger(ConfigurationSection section) {
        Assert.notNull(section, "Can't find .trigger");
        assert section != null;
        Trigger trigger = new Trigger();
        trigger.setSource(Trigger.Source.parse(section.getString("source")));
        trigger.setAdmin(section.getBoolean("admin"));
        trigger.setBind(section.getBoolean("bind"));
        trigger.setType(Trigger.Type.parse(section.getString("type")));
        trigger.setFilter(section.getBoolean("filter"));
        trigger.setKeys(section.getStringList("keys"));
        return trigger;
    }

    private static Image deserializeImage(ConfigurationSection section) {
        Assert.notNull(section, "Can't find .image");
        Image image = new Image();
        image.setSource(section.getString("source"));
        image.setWidth(section.getInt("width"));
        image.setHeight(section.getInt("height"));
        image.setColor(Color.decode(section.getString("color")));
        image.setSize(section.getInt("size"));
        return image;
    }



}
