package com.illtamer.infinite.bot.expansion.landlords.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;

public class ImageUtil {

    private static final String PREFIX = "base64://";

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
