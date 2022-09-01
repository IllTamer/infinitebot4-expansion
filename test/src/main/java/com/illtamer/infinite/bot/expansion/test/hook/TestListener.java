package com.illtamer.infinite.bot.expansion.test.hook;

import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;

public class TestListener implements Listener {

    @EventHandler
    public void onTest(MessageEvent event) {
        if (event.getRawMessage().equals("test")) {
            event.reply("test");
        }
    }

}
