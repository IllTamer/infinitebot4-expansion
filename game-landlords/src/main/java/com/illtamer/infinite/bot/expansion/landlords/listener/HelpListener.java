package com.illtamer.infinite.bot.expansion.landlords.listener;

import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.expansion.landlords.config.Configuration;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.automation.Registration;

public class HelpListener implements Listener {

    private final Configuration configuration;

    public HelpListener() {
        this.configuration = Registration.get(Configuration.class);
    }

    @EventHandler
    public void onHelp(MessageEvent event) {
        if (event.getMessage().isTextOnly() && event.getRawMessage().equals(configuration.getHelp())) {
            event.reply(MessageBuilder.json()
                    .text("关键词列表：")
                    .text(configuration.getJoin() + " # 加入游戏")
                    .text(configuration.getGrab() + " # 抢地主")
                    .text(configuration.getDisgrab() + " # 不抢地主")
                    .text(configuration.getSend() + " # 牌的种类名称为[3 4 5 6 7 8 9 10 J Q K A 2 小王 大王]")
                    .text(configuration.getDissend() + " # 不出牌")
                    .text(configuration.getStop() + " # 中止对局指令 仅管理员可用")
                    .text(configuration.getSee() + " # 查看自身手牌，仅私聊可用")
                    .build());
        }
    }


}
