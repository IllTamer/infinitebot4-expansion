package com.illtamer.infinite.bot.expansion.manager.message.eneity;

import lombok.RequiredArgsConstructor;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CapturingCommandSource implements CommandSource {

    private final List<String> capturedMessages = new ArrayList<>();

    private final CommandSender sender;

    @Override
    public void sendSystemMessage(Component message) {
        System.out.println("sendSystemMessage=");
        // 捕获所有系统消息（包括指令响应）
        String text = message.getString();
        capturedMessages.add(text);
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public CommandSender getBukkitSender(CommandSourceStack commandSourceStack) {
        return sender;
    }

    @Override
    public boolean alwaysAccepts() {
        return CommandSource.super.alwaysAccepts();
    }

    public List<String> getCapturedMessages() {
        return new ArrayList<>(capturedMessages);
    }

    public void clearMessages() {
        capturedMessages.clear();
    }

}