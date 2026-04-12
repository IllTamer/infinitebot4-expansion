package com.illtamer.infinite.bot.expansion.manager.message;

import com.illtamer.infinite.bot.expansion.manager.message.config.MessageConfig;
import com.illtamer.infinite.bot.expansion.manager.message.handler.CommandMessageHandler;
import com.illtamer.infinite.bot.expansion.manager.message.handler.ImageMessageHandler;
import com.illtamer.infinite.bot.expansion.manager.message.handler.MessageResponseSender;
import com.illtamer.infinite.bot.expansion.manager.message.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.manager.message.listener.GroupMessageListener;
import com.illtamer.infinite.bot.expansion.manager.message.render.ImageRenderer;
import com.illtamer.infinite.bot.expansion.manager.message.util.ResourceManager;
import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.expansion.manager.InfiniteExpansion;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class MessageManager extends InfiniteExpansion {

    @Getter
    private static MessageManager instance;
    private ExpansionConfig configFile;
    private Language language;
    private ResourceManager resourceManager;

    @Override
    public void onEnable() {
        instance = this;
        configFile = new ExpansionConfig("config.yml", this);
        this.language = Language.of("language", this);
        Placeholder.init();

        FileConfiguration config = configFile.getConfig();

        // 初始化资源管理器
        String imageDir = config.getString("resources.image-dir", "");
        String fontDir = config.getString("resources.font-dir", "");
        resourceManager = new ResourceManager(imageDir, fontDir, getLogger());
        resourceManager.load();

        // 解析消息配置
        List<MessageConfig> messageConfigs = loadMessageConfigs(config);

        // 初始化处理器
        int delayTick = config.getInt("command-settings.delay-tick", 5);
        String senderName = config.getString("command-settings.sender-name", "InfiniteBot");
        MessageResponseSender responseSender = new MessageResponseSender(getLogger());
        CommandMessageHandler commandHandler = new CommandMessageHandler(delayTick, senderName, responseSender);
        ImageRenderer renderer = new ImageRenderer(resourceManager);
        ImageMessageHandler imageHandler = new ImageMessageHandler(renderer, getLogger(), language, responseSender);

        // 注册监听器
        EventExecutor.registerEvents(
                new GroupMessageListener(messageConfigs, language, responseSender, commandHandler, imageHandler),
                this
        );

        getLogger().info("[MessageManager] 已加载 " + messageConfigs.size() + " 条消息配置");
    }

    @Override
    public void onDisable() {
        if (resourceManager != null) {
            resourceManager.clear();
        }
        instance = null;
    }

    @Override
    public String getExpansionName() {
        return "MessageManager";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public String getAuthor() {
        return "IllTamer";
    }

    private List<MessageConfig> loadMessageConfigs(FileConfiguration config) {
        List<MessageConfig> list = new ArrayList<>();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection == null) return list;

        Set<String> keys = messagesSection.getKeys(false);
        for (String key : keys) {
            ConfigurationSection section = messagesSection.getConfigurationSection(key);
            if (section == null) continue;
            try {
                list.add(new MessageConfig(key, section));
            } catch (Exception e) {
                getLogger().warn("[MessageManager] 加载消息配置失败: " + key + " - " + e.getMessage());
            }
        }
        return list;
    }

}
