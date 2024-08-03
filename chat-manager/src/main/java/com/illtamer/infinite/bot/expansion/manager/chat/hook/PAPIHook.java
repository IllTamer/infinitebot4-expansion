package com.illtamer.infinite.bot.expansion.manager.chat.hook;

import com.illtamer.infinite.bot.expansion.hook.papi.PHandler;
import com.illtamer.infinite.bot.expansion.hook.papi.PHandlerEnum;
import com.illtamer.infinite.bot.expansion.hook.papi.PlaceholderAPIHook;
import com.illtamer.infinite.bot.expansion.hook.papi.StringHelper;
import com.illtamer.infinite.bot.expansion.manager.chat.Global;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PAPIHook {

    // 检查玩家是否监听了某个群（默认true）
    private static final PHandlerEnum CHECK_FORWARD_GROUP = new PHandlerEnum("check-forward-group", (offlinePlayer, arg) -> {
        if (offlinePlayer == null) {
            return PHandler.ONLY_PLAYER;
        }
        Long group;
        if (arg == null || (group = StringHelper.parseLong(arg)) == null) {
            return PHandler.WRONG_ARG;
        }
        String key = Global.gCloseKey(offlinePlayer.getUniqueId().toString(), group.toString());
        Object close = Global.getCloseMap().get(key);
        return Boolean.toString(close == null || !((boolean) close));
    });

    // ib4_get-close-groups
    private static final PHandlerEnum GET_CLOSE_GROUPS = new PHandlerEnum("get-close-groups", (offlinePlayer, arg) -> {
        if (offlinePlayer == null) {
            return PHandler.ONLY_PLAYER;
        }
        String uuid = offlinePlayer.getUniqueId().toString();
        return Global.getCloseMap().entrySet().stream()
                .filter(e -> e.getValue() != null && (boolean) e.getValue())
                .filter(e -> e.getKey().startsWith(uuid))
                .map(e -> Long.parseLong(e.getKey().split("#")[1]))
                .collect(Collectors.toList())
                .toString();
    });

    public static void register() {
        PlaceholderAPIHook hook = (PlaceholderAPIHook) StaticAPI.getExpansion("PlaceholderAPIHook", "IllTamer");
        hook.getHandlerList().add(new PHandlerEnum("sub-id", (offlinePlayer, arg) -> {
            if (offlinePlayer == null) {
                return PHandler.ONLY_PLAYER;
            }
            return "hello world";
        }));
        hook.getHandlerList().addAll(Arrays.asList(CHECK_FORWARD_GROUP, GET_CLOSE_GROUPS));
    }

    public static void unregister() {
        PlaceholderAPIHook hook = (PlaceholderAPIHook) StaticAPI.getExpansion("PlaceholderAPIHook", "IllTamer");
        hook.getHandlerList().removeAll(Arrays.asList(CHECK_FORWARD_GROUP, GET_CLOSE_GROUPS));
    }

}
