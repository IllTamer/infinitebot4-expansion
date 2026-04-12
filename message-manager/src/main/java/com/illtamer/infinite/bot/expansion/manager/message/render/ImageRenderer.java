package com.illtamer.infinite.bot.expansion.manager.message.render;

import com.illtamer.infinite.bot.expansion.manager.message.config.ImageConfig;
import com.illtamer.infinite.bot.expansion.manager.message.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.manager.message.util.ResourceManager;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * 图片渲染引擎，使用 Java2D 合成图片
 */
public class ImageRenderer {

    private final ResourceManager resourceManager;

    public ImageRenderer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * 渲染图片并返回 Base64 编码字符串（用于 CQ 码发送）
     *
     * @param config 图片配置
     * @param player 玩家（用于 placeholder 解析，可为 null）
     * @return base64 字符串，失败返回 null
     */
    public String render(ImageConfig config, OfflinePlayer player) throws IOException {
        BufferedImage canvas = buildCanvas(config);
        if (canvas == null) return null;

        Graphics2D g = canvas.createGraphics();
        // 开启抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 按顺序绘制图层
        for (ImageConfig.LayerConfig layer : config.getLayers()) {
            if ("text".equalsIgnoreCase(layer.getType())) {
                drawTextLayer(g, layer, player);
            } else if ("image".equalsIgnoreCase(layer.getType())) {
                drawImageLayer(g, layer);
            }
        }

        g.dispose();

        // 转为 Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(canvas, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private BufferedImage buildCanvas(ImageConfig config) {
        BufferedImage bg = resourceManager.getImage(config.getBackground());
        int w = config.getWidth() > 0 ? config.getWidth() : (bg != null ? bg.getWidth() : 800);
        int h = config.getHeight() > 0 ? config.getHeight() : (bg != null ? bg.getHeight() : 600);

        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        if (bg != null) {
            g.drawImage(bg, 0, 0, w, h, null);
        } else {
            // 无背景图时填充透明
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, w, h);
        }
        g.dispose();
        return canvas;
    }

    private void drawTextLayer(Graphics2D g, ImageConfig.LayerConfig layer, OfflinePlayer player) {
        String content = layer.getContent();
        if (content == null || content.isEmpty()) return;

        // 文字图层支持 PlaceholderAPI
        content = Placeholder.set(content, player);

        ImageConfig.FontConfig fontCfg = layer.getFont();
        Font font;
        Color color;

        if (fontCfg == null) {
            // 未配置字体时使用默认字体，仍然绘制文字
            font = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
            color = Color.WHITE;
        } else {
            int size = fontCfg.getSize() > 0 ? fontCfg.getSize() : 16;
            int style = parseFontStyle(fontCfg.getStyle());
            String file = fontCfg.getFile();
            if (file == null || file.isBlank()) {
                font = new Font(Font.SANS_SERIF, style, size);
            } else {
                font = resourceManager.getFont(file, size, style);
            }
            color = parseColor(fontCfg.getColor());
        }

        g.setFont(font);
        g.setColor(color);
        g.drawString(content, layer.getX(), layer.getY());
    }

    private void drawImageLayer(Graphics2D g, ImageConfig.LayerConfig layer) {
        BufferedImage img = resourceManager.getImage(layer.getFile());
        if (img == null) return;

        int w = layer.getWidth() > 0 ? layer.getWidth() : img.getWidth();
        int h = layer.getHeight() > 0 ? layer.getHeight() : img.getHeight();
        g.drawImage(img, layer.getX(), layer.getY(), w, h, null);
    }

    private int parseFontStyle(String style) {
        if (style == null) return Font.PLAIN;
        switch (style.toLowerCase()) {
            case "bold": return Font.BOLD;
            case "italic": return Font.ITALIC;
            case "bold_italic": return Font.BOLD | Font.ITALIC;
            default: return Font.PLAIN;
        }
    }

    private Color parseColor(String hex) {
        if (hex == null || hex.isEmpty()) return Color.WHITE;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            if (clean.length() == 6) {
                return new Color(Integer.parseInt(clean, 16));
            } else if (clean.length() == 8) {
                // AARRGGBB
                int a = Integer.parseInt(clean.substring(0, 2), 16);
                int r = Integer.parseInt(clean.substring(2, 4), 16);
                int gr = Integer.parseInt(clean.substring(4, 6), 16);
                int b = Integer.parseInt(clean.substring(6, 8), 16);
                return new Color(r, gr, b, a);
            }
        } catch (NumberFormatException ignored) {}
        return Color.WHITE;
    }

}
