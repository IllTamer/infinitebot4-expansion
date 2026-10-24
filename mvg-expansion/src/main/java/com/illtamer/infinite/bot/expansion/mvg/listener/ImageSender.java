package com.illtamer.infinite.bot.expansion.mvg.listener;

import com.illtamer.infinite.bot.expansion.mvg.client.MvgRenderResult;
import com.illtamer.infinite.bot.expansion.mvg.config.MvgConfig;
import com.illtamer.perpetua.sdk.message.Message;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.UUID;

/**
 * 渲染结果的图片发送方式
 * <p>
 * 支持两种模式（{@code render.image-upload-mode}）：
 * <ul>
 *   <li>{@code base64}：直接把图片以 {@code base64://} 形式塞进消息段，无需落盘，
 *       适用于 bot 与服务器不同机部署；</li>
 *   <li>{@code file}：先把 PNG 写入附属数据目录，再把绝对路径交给 bot，
 *       适用于 bot 与服务器同机且希望复用本地文件的场景。</li>
 * </ul>
 * */
public class ImageSender {

    /** bot 侧 onebot 实现识别的 base64 前缀 */
    private static final String BASE64_PREFIX = "base64://";

    private final MvgConfig config;
    private final File cacheFolder;
    private final Logger logger;

    public ImageSender(MvgConfig config, File dataFolder, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.cacheFolder = new File(dataFolder, "cache");
        if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
            logger.warn("[Render] 无法创建图片缓存目录: " + cacheFolder.getAbsolutePath());
        }
    }

    /**
     * 构造只包含一张图片的消息
     * */
    public Message build(MvgRenderResult result) throws IOException {
        if (config.useFileImage()) {
            File file = writeToFile(result);
            return MessageBuilder.json().image(file.getAbsolutePath(), null).build();
        }
        return MessageBuilder.json().image(BASE64_PREFIX + result.getData(), null).build();
    }

    private File writeToFile(MvgRenderResult result) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(result.getData());
        File file = new File(cacheFolder, "render-" + UUID.randomUUID() + ".png");
        Files.write(file.toPath(), bytes);
        cleanup();
        return file;
    }

    /**
     * 只保留最近 {@code render.image-cache-size} 个图片文件
     * */
    private void cleanup() {
        File[] files = cacheFolder.listFiles((dir, name) -> name.startsWith("render-") && name.endsWith(".png"));
        if (files == null || files.length <= config.imageCacheSize) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int removeCount = files.length - config.imageCacheSize;
        for (int index = 0; index < removeCount; index++) {
            if (!files[index].delete()) {
                logger.debug("[Render] 无法删除过期图片: " + files[index].getAbsolutePath());
            }
        }
    }

    /**
     * 附属卸载时收敛落盘的图片
     * <p>
     * 只做一次数量裁剪而不是全部删除：bot 侧是拿到绝对路径后异步读取上传的，
     * 卸载瞬间删光会让尚未发出的图片丢失
     * */
    public void clear() {
        cleanup();
    }

}
