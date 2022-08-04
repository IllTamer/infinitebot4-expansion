package com.illtamer.infinite.bot.expansion.view.util;

import com.loohp.interactivechat.utils.HTTPRequestUtils;
import com.loohp.interactivechatdiscordsrvaddon.InteractiveChatDiscordSrvAddon;
import com.loohp.interactivechatdiscordsrvaddon.api.events.DiscordAttachmentConversionEvent;
import com.loohp.interactivechatdiscordsrvaddon.graphics.GifReader;
import com.loohp.interactivechatdiscordsrvaddon.listeners.InboundToGameEvents;
import com.loohp.interactivechatdiscordsrvaddon.utils.URLRequestUtils;
import com.loohp.interactivechatdiscordsrvaddon.wrappers.GraphicsToPacketMapWrapper;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class DispatchUtil {

    private DispatchUtil() {}

    @Nullable
    public static UUID executeImageWrapper(@NotNull String imageUrl, boolean gif) {
        final String[] split = imageUrl.split("\\?");
        String url = split[0];
        // TODO check processedUrl & whitelistUrl
        long size = HTTPRequestUtils.getContentSize(url);
        if (size >= 0 && size <= InteractiveChatDiscordSrvAddon.plugin.discordAttachmentsPreviewLimit) {
            InteractiveChatDiscordSrvAddon.plugin.attachmentImageCounter.incrementAndGet();
            try (InputStream stream = URLRequestUtils.getInputStream(url)) {
                GraphicsToPacketMapWrapper map;
                if (gif || url.toLowerCase().endsWith(".gif")) {
                    map = new GraphicsToPacketMapWrapper(InteractiveChatDiscordSrvAddon.plugin.playbackBarEnabled, InteractiveChatDiscordSrvAddon.plugin.discordAttachmentsMapBackgroundColor);
                    GifReader.readGif(stream, InteractiveChatDiscordSrvAddon.plugin.mediaReadingService, (frames, e) -> {
                        if (e != null) {
                            e.printStackTrace();
                            map.completeFuture(null);
                        } else {
                            map.completeFuture(frames);
                        }
                    });
                } else {
                    BufferedImage image = ImageIO.read(stream);
                    map = new GraphicsToPacketMapWrapper(image, InteractiveChatDiscordSrvAddon.plugin.discordAttachmentsMapBackgroundColor);
                }
                int end = url.lastIndexOf("/");
                String name = end < 0 ? url : url.substring(end + 1);
                InboundToGameEvents.DiscordAttachmentData data = new InboundToGameEvents.DiscordAttachmentData(name, url, map, false);
                DiscordAttachmentConversionEvent dace = new DiscordAttachmentConversionEvent(url, data);
                Bukkit.getPluginManager().callEvent(dace);
                InboundToGameEvents.DATA.put(data.getUniqueId(), data);
                Bukkit.getScheduler().runTaskLater(InteractiveChatDiscordSrvAddon.plugin, () -> InboundToGameEvents.DATA.remove(data.getUniqueId()), InteractiveChatDiscordSrvAddon.plugin.discordAttachmentTimeout);
                return data.getUniqueId();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
