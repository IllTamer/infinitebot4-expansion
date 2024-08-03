package com.illtamer.infinite.bot.expansion.parse.video.listener;

import com.illtamer.infinite.bot.expansion.parse.video.ParseVideoExpansion;
import com.illtamer.infinite.bot.expansion.parse.video.api.BiliVideo;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.message.Message;

import java.io.InterruptedIOException;

public class CommandListener implements Listener {

    @EventHandler
    public void onCommand(MessageEvent event) {
        if (!event.getMessage().isTextOnly()) return;
        String msg = event.getRawMessage();
        if (!msg.startsWith("/parse ")) return;

        String[] args = msg.split(" ");

        if (args.length != 3) {
            event.reply("输入指令错误，应为 '/parse <type> <url>'\n输入 '/parse-types' 查看支持解析的连接类型");
            return;
        }
        switch (args[1]) {
            case "bili": {
                try {
                    Message message = BiliVideo.getBiliURL(args[2]);
                    event.reply(message);
                } catch (Exception e) {
                    if (e instanceof InterruptedIOException)
                        System.out.println("API 调用超时: " + e.getMessage());
                    else {
                        ParseVideoExpansion.getInstance().getLogger().warn("解析视频链接失败: " + args[2], e);
                        event.reply("视频解析失败");
                    }
                }
                break;
            }
            default: {
                event.reply("未知的解析类型: " + args[1]);
            }
        }
    }

}
