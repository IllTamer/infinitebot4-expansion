package com.illtamer.infinite.bot.expansion.mvg.distribute;

import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.expansion.mvg.MvgExpansion;
import com.illtamer.infinite.bot.expansion.mvg.render.InventorySlotMapper;
import com.illtamer.infinite.bot.expansion.mvg.render.ItemStackCodec;
import com.illtamer.infinite.bot.expansion.mvg.render.TooltipBuilder;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinData;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinResolver;
import com.illtamer.infinite.bot.expansion.mvg.util.MainThreadCaller;
import com.illtamer.infinite.bot.minecraft.api.distribute.AbstractDistributedListener;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedEventContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 跨服玩家实时数据采集
 * <p>
 * 主节点收到查询请求后向所有子服广播，只有玩家真正所在的子服会返回 {@code found=true} 的数据。
 * 采集逻辑整体跑在主线程（{@link MainThreadCaller#call}），
 * 且只读取已缓存的皮肤，不发起任何网络请求 —— 分布式回包等待上限只有 3 秒。
 * <p>
 * 同一份采集逻辑也对外暴露为 {@link #queryLocal}，供主节点在跨服链路不可用时降级为单机查询。
 * */
public class MvgPlayerQueryListener extends AbstractDistributedListener<PlayerSnapshotPayload> {

    public static final String IDENTIFIER = "playerSnapshot";

    public static final String PARAM_ACTION = "action";
    public static final String PARAM_UUID = "uuid";
    public static final String PARAM_VALID_UUID = "validUuid";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_HAND = "hand";
    public static final String PARAM_NEED_SKIN = "needSkin";

    /** 末影箱固定 3 行 9 列 */
    private static final int ENDER_CHEST_SLOTS = 27;

    private final MvgExpansion expansion;
    private final SkinResolver skinResolver;
    private final TooltipBuilder tooltipBuilder;

    public MvgPlayerQueryListener(MvgExpansion expansion, SkinResolver skinResolver,
                                  TooltipBuilder tooltipBuilder) {
        super(expansion, PlayerSnapshotPayload.class);
        this.expansion = expansion;
        this.skinResolver = skinResolver;
        this.tooltipBuilder = tooltipBuilder;
    }

    @Override
    public PlayerSnapshotPayload handle(DistributedEventContext context) {
        return queryLocal(context);
    }

    /**
     * 在本服采集目标玩家的实时数据
     * <p>
     * 既是分布式广播的响应实现，也是主节点"跨服不可用时回退单机"的入口。
     * <p>
     * 任何异常（含 {@link NoClassDefFoundError} 这类链接错误、主线程调度失败）都必须转成
     * error 回包，绝不能向外抛出：旧版 ib4 的广播接收端没有兜底，
     * 一旦抛出主节点就只能等到 3 秒超时，并且整条查询链路不会给出任何响应。
     * */
    public PlayerSnapshotPayload queryLocal(DistributedEventContext context) {
        final String action = context.getStringParam(PARAM_ACTION);
        final String uuid = context.getStringParam(PARAM_UUID);
        final String validUuid = context.getStringParam(PARAM_VALID_UUID);
        final String name = context.getStringParam(PARAM_NAME);
        final String hand = context.getStringParam(PARAM_HAND);
        final boolean needSkin = Boolean.TRUE.equals(context.getParam(PARAM_NEED_SKIN));
        final String serverName = expansion.getServerName();
        try {
            PlayerSnapshotPayload payload = MainThreadCaller.call(
                    () -> collect(action, uuid, validUuid, name, hand, needSkin, serverName));
            return payload == null ? PlayerSnapshotPayload.notFound(serverName) : payload;
        } catch (Throwable e) {
            expansion.getLogger().warn("[Distribute] 采集玩家数据失败: "
                    + (name == null || name.isEmpty() ? uuid : name), e);
            return PlayerSnapshotPayload.error(serverName, e.toString());
        }
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    private PlayerSnapshotPayload collect(String action, String uuid, String validUuid, String name,
                                          String hand, boolean needSkin, String serverName) {
        Player player = findPlayer(uuid, validUuid, name);
        if (player == null) {
            return PlayerSnapshotPayload.notFound(serverName);
        }
        PlayerSnapshotPayload payload = new PlayerSnapshotPayload();
        payload.setFound(true);
        payload.setServerName(serverName);
        payload.setPlayerName(player.getName());
        payload.setPlayerUuid(player.getUniqueId().toString());

        String resolvedAction = action == null ? PlayerSnapshotPayload.ACTION_INVENTORY : action;
        switch (resolvedAction) {
            case PlayerSnapshotPayload.ACTION_HAND:
                // tooltip 必须在这里生成：只有玩家所在的子服能读到真实的 ItemStack
                payload.setMainHandTooltip(tooltipBuilder.build(player, player.getInventory().getItemInMainHand()));
                payload.setOffHandTooltip(tooltipBuilder.build(player, player.getInventory().getItemInOffHand()));
                break;
            case PlayerSnapshotPayload.ACTION_ENDER_CHEST:
                payload.setSlots(InventorySlotMapper.containerSlots(player.getEnderChest(), ENDER_CHEST_SLOTS));
                break;
            case PlayerSnapshotPayload.ACTION_INVENTORY:
            default:
                payload.setSlots(InventorySlotMapper.storageSlots(player.getInventory()));
                payload.setArmor(InventorySlotMapper.armorSlots(player.getInventory()));
                payload.setMainHand(nonEmpty(ItemStackCodec.toItem(
                        player.getInventory().getItemInMainHand(), false)));
                payload.setOffHand(nonEmpty(ItemStackCodec.toItem(
                        player.getInventory().getItemInOffHand(), false)));
                break;
        }
        if (needSkin) {
            SkinData skin = skinResolver.getCachedOrDefault(player.getUniqueId(), null);
            payload.setSkinDataUri(skin.getDataUri());
            payload.setSkinModel(skin.getModel());
            expansion.getLogger().debug("[Skin] " + player.getName() + " 使用 " + skin.getSource() + " 皮肤");
        }
        return payload;
    }

    /**
     * 空物品直接返回 null，避免渲染层把空气当物品处理
     * */
    private static JsonObject nonEmpty(JsonObject item) {
        if (item == null || !item.has("id")) {
            return null;
        }
        return item;
    }

    /**
     * 依次按离线 UUID、正版 UUID、玩家名匹配本服在线玩家
     * <p>
     * 绑定数据里两个 UUID 都可能存在（离线 UUID 与正版 UUID），
     * 而玩家实际登录时使用的那个取决于服务器是否开启正版验证，因此两个都要试。
     * */
    private Player findPlayer(String uuid, String validUuid, String name) {
        Player byUuid = byUuid(uuid);
        if (byUuid != null) {
            return byUuid;
        }
        Player byValidUuid = byUuid(validUuid);
        if (byValidUuid != null) {
            return byValidUuid;
        }
        if (name != null && !name.isEmpty()) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                return player;
            }
        }
        return null;
    }

    private Player byUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        try {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            return player != null && player.isOnline() ? player : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
