package com.illtamer.infinite.bot.expansion.manager.chat;

import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class Global {

    public static final String PATH = "close-list";

    private static Map<String, Object> closeMap;
    private static ChatManager instance;

    private Global() {}

    public static void init(ChatManager _instance) {
        instance = _instance;
    }

    public static Map<String, Object> getCloseMap() {
        if (closeMap == null) {
            final FileConfiguration config = instance.getCloseFile().getConfig();
            ConfigurationSection section = config.getConfigurationSection(PATH);
            if (section == null)
                section = config.createSection(PATH);
            closeMap = section.getValues(false);
        }
        return closeMap;
    }

    public static void save(ExpansionConfig closeFile) {
        closeFile.getConfig().set(PATH, closeMap);
        closeFile.save();
    }

    public static String gCloseKey(String uuid, String groupId) {
        return new StringBuffer()
                .append(uuid)
                .append("#")
                .append(groupId)
                .toString();
    }

}
