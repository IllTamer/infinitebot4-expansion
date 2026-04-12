package com.illtamer.infinite.bot.expansion.manager.message.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * 单条消息配置，对应 config.yml messages 下的一个节点
 */
@Getter
public class MessageConfig {

    public enum MessageType {
        TEXT,       // 纯文本
        COMMAND,    // 指令
        IMAGE,      // 纯图片
        COMBINED    // 文字+图片组合
    }

    private final String name;
    private final ConditionConfig condition;
    private final PermissionConfig permission;
    private final boolean onlyBind;
    /**
     * 是否使用回复消息（true=reply，false=普通群消息）
     */
    private final boolean reply;

    // text
    private final List<String> text;

    // command
    private final String commandIdentity;
    private final List<String> commandList;

    // image
    private final ImageConfig image;

    private final MessageType type;

    public MessageConfig(String name, ConfigurationSection section) {
        this.name = name;
        this.condition = new ConditionConfig(section.getConfigurationSection("condition") != null
                ? section.getConfigurationSection("condition")
                : section.createSection("condition"));
        ConfigurationSection permSection = section.getConfigurationSection("permission");
        this.permission = permSection != null ? new PermissionConfig(permSection) : new PermissionConfig(section.createSection("permission"));
        this.onlyBind = section.getBoolean("only-bind", false);
        this.reply = section.getBoolean("reply", true);

        this.text = section.getStringList("text");

        ConfigurationSection cmdSection = section.getConfigurationSection("command");
        if (cmdSection != null) {
            this.commandIdentity = cmdSection.getString("identity", "console");
            this.commandList = cmdSection.getStringList("list");
        } else {
            this.commandIdentity = null;
            this.commandList = null;
        }

        ConfigurationSection imgSection = section.getConfigurationSection("image");
        this.image = imgSection != null ? new ImageConfig(imgSection) : null;

        // 判断消息类型
        boolean hasText = !text.isEmpty();
        boolean hasCommand = commandList != null && !commandList.isEmpty();
        boolean hasImage = image != null;

        if (hasCommand) {
            this.type = MessageType.COMMAND;
        } else if (hasImage && hasText) {
            this.type = MessageType.COMBINED;
        } else if (hasImage) {
            this.type = MessageType.IMAGE;
        } else {
            this.type = MessageType.TEXT;
        }
    }

}
