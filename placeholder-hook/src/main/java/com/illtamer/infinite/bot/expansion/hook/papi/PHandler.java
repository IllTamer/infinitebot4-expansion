package com.illtamer.infinite.bot.expansion.hook.papi;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public interface PHandler {

    String ONLY_PLAYER = "仅玩家可用";
    String UNBIND = "未绑定";
    String WRONG_ARG = "传入参数错误";
    String NO_DATA = "无数据";

    /**
     * @param arg 去掉子标识符后传入的变量
     * */
    String set(@Nullable OfflinePlayer player, @Nullable String arg);

}
