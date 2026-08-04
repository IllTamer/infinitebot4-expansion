package com.illtamer.infinite.bot.expansion.hook.papi.hook;

import com.illtamer.infinite.bot.expansion.hook.papi.PHandlerEnum;
import com.illtamer.infinite.bot.expansion.hook.papi.PlaceholderAPIHook;
import com.illtamer.infinite.bot.minecraft.api.scheduler.MinecraftScheduler;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PAPIHook extends PlaceholderExpansion {

    private static PAPIHook instance;

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        for (PHandlerEnum pHEnum : PlaceholderAPIHook.getInstance().getHandlerList()) {
            String subId = pHEnum.getSubId();
            if (params.startsWith(subId)) {
                String arg = params.substring(subId.length());
                if (!arg.startsWith("_")) {
                    arg = null;
                } else {
                    arg = arg.substring("_".length());
                }
                return pHEnum.getPHandler().set(player, arg);
            }
        }
        return null;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ib4";
    }

    @Override
    public @NotNull String getAuthor() {
        return "IllTamer";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    /**
     * 尝试注册 papi 附属
     */
    public static boolean tryRegister() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return false;
        }

        if (MinecraftScheduler.isGlobalThread()) {
            return registerNow();
        }

        MinecraftScheduler.runTask(PAPIHook::registerNow);
        return true;
    }

    public static void tryUnregister() {
        PAPIHook hook = instance;
        if (hook == null) {
            return;
        }

        if (MinecraftScheduler.isGlobalThread()) {
            unregisterNow(hook);
            return;
        }

        MinecraftScheduler.runTask(() -> unregisterNow(hook));
    }

    private static boolean registerNow() {
        if (instance != null) {
            return true;
        }

        PAPIHook hook = new PAPIHook();
        if (!hook.register()) {
            return false;
        }

        instance = hook;
        return true;
    }

    private static void unregisterNow(PAPIHook hook) {
        try {
            hook.unregister();
        } finally {
            if (instance == hook) {
                instance = null;
            }
        }
    }

}
