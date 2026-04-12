package com.illtamer.infinite.bot.expansion.manager.message.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ImageConfig {

    private final String background;
    private final int width;
    private final int height;
    private final List<LayerConfig> layers;

    public ImageConfig(ConfigurationSection section) {
        this.background = section.getString("background", "");
        this.width = section.getInt("width", -1);
        this.height = section.getInt("height", -1);
        this.layers = new ArrayList<>();
        List<?> layerList = section.getList("layers");
        if (layerList != null) {
            for (Object obj : layerList) {
                if (obj instanceof ConfigurationSection) {
                    layers.add(new LayerConfig((ConfigurationSection) obj));
                }
            }
        }
        // 兼容 ConfigurationSection 方式读取 layers
        ConfigurationSection layersSection = section.getConfigurationSection("layers");
        if (layersSection == null && layers.isEmpty()) {
            // 尝试通过 getMapList 读取
            List<java.util.Map<?, ?>> mapList = section.getMapList("layers");
            for (java.util.Map<?, ?> map : mapList) {
                layers.add(LayerConfig.fromMap(map));
            }
        }
    }

    @Getter
    public static class LayerConfig {
        private final String type; // "text" or "image"
        // text fields
        private final String content;
        private final FontConfig font;
        // image fields
        private final String file;
        private final int width;
        private final int height;
        // common
        private final int x;
        private final int y;

        public LayerConfig(ConfigurationSection section) {
            this.type = section.getString("type", "text");
            this.x = section.getInt("x", 0);
            this.y = section.getInt("y", 0);
            this.content = section.getString("content", "");
            ConfigurationSection fontSection = section.getConfigurationSection("font");
            this.font = fontSection != null ? new FontConfig(fontSection) : null;
            this.file = section.getString("file", "");
            this.width = section.getInt("width", -1);
            this.height = section.getInt("height", -1);
        }

        @SuppressWarnings("unchecked")
        public static LayerConfig fromMap(java.util.Map<?, ?> map) {
            org.bukkit.configuration.MemoryConfiguration mc = new org.bukkit.configuration.MemoryConfiguration();
            applyMap(mc, "", map);
            return new LayerConfig(mc);
        }

        @SuppressWarnings("unchecked")
        private static void applyMap(org.bukkit.configuration.MemoryConfiguration mc, String prefix, java.util.Map<?, ?> map) {
            for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                Object value = entry.getValue();
                String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
                if (value instanceof java.util.Map<?, ?> childMap) {
                    applyMap(mc, fullKey, childMap);
                } else {
                    mc.set(fullKey, value);
                }
            }
        }
    }

    @Getter
    public static class FontConfig {
        private final String file;
        private final int size;
        private final String color;
        private final String style;

        public FontConfig(ConfigurationSection section) {
            this.file = section.getString("file", "");
            this.size = section.getInt("size", 16);
            this.color = section.getString("color", "#FFFFFF");
            this.style = section.getString("style", "plain");
        }
    }

}
