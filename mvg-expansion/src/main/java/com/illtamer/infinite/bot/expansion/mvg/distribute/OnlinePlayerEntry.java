package com.illtamer.infinite.bot.expansion.mvg.distribute;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 单个在线玩家条目，供 mvg 的 playerlist 渲染使用
 * */
@Data
@NoArgsConstructor
public class OnlinePlayerEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String uuid;

    /**
     * 显示名（TAB 列表中的名字，默认与 {@link #name} 一致）
     * */
    private String display;

    /**
     * 连接延迟（毫秒），mvg 按原版阈值选择 ping 图标
     * */
    private int ping;

    /**
     * 游戏模式名（survival / creative / adventure / spectator）
     * <p>
     * mvg 接受并校验该字段，但与原版一致地不绘制
     * */
    private String gameMode;

    /**
     * 头像贴图 data URI（内联 64×64 PNG），mvg 的 playerlist 要求每个玩家都必须提供
     * */
    private String texture;

    /**
     * 所在子服名称，仅用于分组展示与提示
     * */
    private String serverName;

}
