package com.illtamer.infinite.bot.expansion.message.util;

import com.illtamer.infinite.bot.expansion.message.InputStreamSupplier;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class ImageUtil {

    private static final String PREFIX = "base64://";

    /**
     * @param width 图片宽
     * @param height 图片高
     * @param startX 起始x坐标
     * @param space 行间距
     * @param fontColor 字体颜色
     * @param font 字体
     * */
    public static String draw(Consumer<Graphics> consumer, InputStreamSupplier supplier, int width, int height,
                              int startX, int startY, int space, Color fontColor, Font font, List<String> lines) {
        try (InputStream input = supplier.get()) {
            return draw(width, height, startX, startY, space, lines, font, fontColor, input, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String draw(int width, int height, int startX, int startY, int space, List<String> lines,
                               Font font, Color fontColor, InputStream input, Consumer<Graphics> consumer) throws IOException {
        BufferedImage image = ImageIO.read(input);
        if (width != 0 && height != 0) {
            image = toBufferedImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        }
        Graphics graphics = image.getGraphics();
        consumer.accept(graphics);
        graphics.setColor(fontColor);
        graphics.setFont(font);
        final int size = font.getSize();
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(lines.get(i), startX, size + startY + (size + space) * i);
        }
        graphics.dispose();
        return imageToBase64(image);
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        //boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
           /* if (hasAlpha) {
             transparency = Transparency.BITMASK;
             }*/

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                    image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            //int type = BufferedImage.TYPE_3BYTE_BGR;//by wang
        /*if (hasAlpha) {
         type = BufferedImage.TYPE_INT_ARGB;
         }*/
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

    public static String imageToBase64(BufferedImage image) {
        return streamToBase64(output -> {
            try {
                ImageIO.write(image, "png", output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static String streamToBase64(Consumer<ByteArrayOutputStream> outputConsumer) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        outputConsumer.accept(output);
        return PREFIX + Base64.getEncoder().encodeToString(output.toByteArray());
    }

}
