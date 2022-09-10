package com.illtamer.infinite.bot.expansion.basic.listener;

import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.api.util.Assert;
import com.illtamer.infinite.bot.expansion.basic.BasicManager;
import com.illtamer.infinite.bot.expansion.basic.entity.SubmitSender;
import com.illtamer.infinite.bot.minecraft.Bootstrap;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

public class SubmitListener implements Listener {

    private final String prefix;
    private final int delayTick;
    private final String senderName;
    private final Language language;

    public SubmitListener(ExpansionConfig configFile, Language language) {
        final FileConfiguration config = configFile.getConfig();
        this.prefix = config.getString("submit.prefix");
        this.delayTick = config.getInt("submit.delay-tick", 5);
        this.senderName = config.getString("submit.sender-name", "InfiniteBot-BasicManager#SubmitSender");
        this.language = language;
        Assert.notNull(prefix, "'submit.prefix' can not be null !");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSubmit(MessageEvent event) {
        if (StaticAPI.isAdmin(event.getSender().getUserId())) {
            final String rawMessage = event.getRawMessage();
            if (rawMessage.startsWith(prefix) && rawMessage.length() >= prefix.length() +  2) {
                String command = rawMessage.substring(prefix.length() + 1);
                final String reply = language.get("submit", "reply");
                if (reply.length() != 0)
                    event.reply(reply);
                SubmitSender sender = new SubmitSender(Bootstrap.getInstance().getServer(), event, delayTick, senderName);
                // 主线程执行指令
                Bukkit.getScheduler().runTask(Bootstrap.getInstance(),
                        () -> Bukkit.dispatchCommand(sender, command));
                BasicManager.getInstance().getLogger().info(String.format(language.get("submit", "log"), event.getSender().getUserId(), command));
                event.setCancelled(true);
            }
        }
    }

}
