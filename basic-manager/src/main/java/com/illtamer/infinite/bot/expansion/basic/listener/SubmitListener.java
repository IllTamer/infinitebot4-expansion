package com.illtamer.infinite.bot.expansion.basic.listener;

import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.basic.BasicManager;
import com.illtamer.infinite.bot.expansion.basic.entity.SubmitSender;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import org.bukkit.Bukkit;

public class SubmitListener implements Listener {

    private final String prefix;

    public SubmitListener(ExpansionConfig configFile) {
        this.prefix = configFile.getConfig().getString("submit.prefix");
        Assert.notNull(prefix, "'submit.prefix' can not be null !");
    }

    @EventHandler
    public void onSubmit(MessageEvent event) {
        if (StaticAPI.isAdmin(event.getSender().getUserId())) {
            final String rawMessage = event.getRawMessage();
            if (rawMessage.startsWith(prefix) && rawMessage.length() >= prefix.length() +  2) {
                String command = rawMessage.substring(prefix.length() + 1);
                event.reply("指令执行中, 请稍后");
                SubmitSender sender = new SubmitSender(Bootstrap.getInstance().getServer(), event);
                // 主线程执行指令
                Bukkit.getScheduler().runTask(Bootstrap.getInstance(),
                        () -> Bukkit.dispatchCommand(sender, command));
                BasicManager.getInstance().getLogger().info(String.format("Id [%s] dispatched a command '%s'", event.getSender().getUserId(), command));
            }
        }
    }

}
