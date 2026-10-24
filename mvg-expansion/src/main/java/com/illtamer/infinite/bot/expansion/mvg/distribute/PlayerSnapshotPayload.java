package com.illtamer.infinite.bot.expansion.mvg.distribute;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.expansion.mvg.render.InventorySlotMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 玩家实时数据的跨服传输载体
 * <p>
 * 群组服中玩家可能位于任意子服，因此主节点通过分布式广播向所有子服索取数据；
 * 本对象即子服回传的内容，字段全部是 gson 可序列化的类型。
 * <p>
 * 注意：广播等待时间被 {@code DistributedEventProcessor} 硬编码为 3 秒，
 * 因此子服在填充本对象时只能做纯内存操作，不得发起任何网络请求。
 * */
@Data
@NoArgsConstructor
public class PlayerSnapshotPayload implements Serializable {

    /** 查询动作：背包 / 手持物 / 末影箱 */
    public static final String ACTION_INVENTORY = "inventory";
    public static final String ACTION_HAND = "hand";
    public static final String ACTION_ENDER_CHEST = "enderchest";

    /** 手持物查询目标：主手 / 副手 */
    public static final String HAND_MAIN = "main";
    public static final String HAND_OFF = "off";

    private static final long serialVersionUID = 1L;

    /**
     * 子服是否找到了目标玩家
     * */
    private boolean found;

    /**
     * 采集失败原因（玩家不在本服时为空）
     * */
    private String error;

    /**
     * 子服名称，用于提示玩家所在位置
     * */
    private String serverName;

    private String playerName;

    private String playerUuid;

    /**
     * 皮肤 data URI（内联 64×64 PNG），mvg 不接受网络 URL
     * */
    private String skinDataUri;

    /**
     * 皮肤模型：classic / slim
     * */
    private String skinModel;

    /**
     * 容器槽位（{@link InventorySlotMapper} 已换算为 mvg 的槽位编号）
     * <p>
     * 背包查询时为玩家物品栏，末影箱查询时为末影箱内容
     * */
    private JsonArray slots;

    /**
     * 装备（顺序固定为 头盔 / 胸甲 / 护腿 / 靴子）
     * */
    private JsonArray armor;

    /**
     * 副手物品（背包渲染的 3D 手持物）
     * */
    private JsonObject offHand;

    /**
     * 主手物品（背包渲染的 3D 手持物）
     * */
    private JsonObject mainHand;

    /**
     * 副手物品的 tooltip 数据
     * <p>
     * 中文 tooltip 的行文本必须由玩家所在的子服生成（只有那里能访问真实的 ItemStack），
     * 因此这里直接携带已经组装完成的 mvg tooltip data
     * */
    private JsonObject offHandTooltip;

    /**
     * 主手物品的 tooltip 数据
     * */
    private JsonObject mainHandTooltip;



    public static PlayerSnapshotPayload notFound(String serverName) {
        PlayerSnapshotPayload payload = new PlayerSnapshotPayload();
        payload.setFound(false);
        payload.setServerName(serverName);
        return payload;
    }

    public static PlayerSnapshotPayload error(String serverName, String error) {
        PlayerSnapshotPayload payload = new PlayerSnapshotPayload();
        payload.setFound(false);
        payload.setServerName(serverName);
        payload.setError(error);
        return payload;
    }

}
