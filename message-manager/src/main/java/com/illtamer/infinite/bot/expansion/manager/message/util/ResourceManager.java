package com.illtamer.infinite.bot.expansion.manager.message.util;

import com.illtamer.infinite.bot.minecraft.expansion.ExpansionLogger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源管理器，负责加载和缓存图片与字体资源
 */
public class ResourceManager {

    private final Map<String, BufferedImage> imageCache = new HashMap<>();
    private final Map<String, Font> fontBaseCache = new HashMap<>();

    private final File imageDir;
    private final File fontDir;
    private final ExpansionLogger logger;

    public ResourceManager(String imageDirPath, String fontDirPath, ExpansionLogger logger) {
        this.imageDir = new File(imageDirPath);
        this.fontDir = new File(fontDirPath);
        this.logger = logger;
    }

    public void load() {
        logger.info("[MessageManager] 图片资源目录: " + imageDir.getAbsolutePath());
        logger.info("[MessageManager] 字体资源目录: " + fontDir.getAbsolutePath());

        loadImages();
        loadFonts();
    }

    private void loadImages() {
        if (!imageDir.exists() || !imageDir.isDirectory()) {
            logger.warn("[MessageManager] 图片目录不存在: " + imageDir.getAbsolutePath());
            return;
        }
        File[] files = imageDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".gif");
        });
        if (files == null) return;
        for (File file : files) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    imageCache.put(file.getName(), img);
                    logger.info("[MessageManager] 已加载图片: " + file.getName());
                }
            } catch (IOException e) {
                logger.warn("[MessageManager] 加载图片失败: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    private void loadFonts() {
        if (!fontDir.exists() || !fontDir.isDirectory()) {
            logger.warn("[MessageManager] 字体目录不存在: " + fontDir.getAbsolutePath());
            return;
        }
        File[] files = fontDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".ttf") || lower.endsWith(".otf");
        });
        if (files == null) return;
        for (File file : files) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, file);
                fontBaseCache.put(file.getName(), font);
                logger.info("[MessageManager] 已加载字体: " + file.getName());
            } catch (FontFormatException | IOException e) {
                logger.warn("[MessageManager] 加载字体失败: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * 获取图片，未找到返回 null
     */
    public BufferedImage getImage(String fileName) {
        return imageCache.get(fileName);
    }

    /**
     * 获取字体（派生指定大小和样式），未找到返回系统默认字体
     */
    public Font getFont(String fileName, int size, int style) {
        if (fileName == null || fileName.isBlank()) {
            return new Font(Font.SANS_SERIF, style, size);
        }
        Font base = fontBaseCache.get(fileName);
        if (base == null) {
            logger.warn("[MessageManager] 字体未找到: " + fileName + "，使用默认字体");
            return new Font(Font.SANS_SERIF, style, size);
        }
        return base.deriveFont(style, (float) size);
    }

    public void clear() {
        imageCache.clear();
        fontBaseCache.clear();
    }

}
