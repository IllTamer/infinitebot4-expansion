package com.illtamer.infinite.bot.expansion.mvg.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgApiException;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgClient;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgRenderResult;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgResourceWarmer;
import com.illtamer.infinite.bot.expansion.mvg.config.MvgConfig;
import com.illtamer.infinite.bot.expansion.mvg.distribute.OnlinePlayerEntry;
import com.illtamer.infinite.bot.expansion.mvg.distribute.PlayerSnapshotPayload;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinData;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinResolver;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 渲染请求组装
 * <p>
 * 把采集到的玩家数据转换为 mvg 的统一渲染报文。所有方法均为阻塞调用，
 * 必须运行在异步线程；渲染前会校验资源预热状态。
 * */
public class MvgRenderService {

    /** mvg 渲染类型 */
    private static final String TYPE_PLAYER_INVENTORY = "playerInventory";
    private static final String TYPE_ENDER_CHEST = "enderchest";
    private static final String TYPE_TOOLTIP = "tooltip";
    private static final String TYPE_PLAYER_LIST = "playerlist";

    private static final int ENDER_CHEST_ROWS = 3;
    private static final int ENDER_CHEST_COLUMNS = 9;

    private final MvgClient client;
    private final MvgResourceWarmer warmer;
    private final MvgConfig config;
    private final SkinResolver skinResolver;
    private final Logger logger;

    public MvgRenderService(MvgClient client, MvgResourceWarmer warmer, MvgConfig config,
                            SkinResolver skinResolver, Logger logger) {
        this.client = client;
        this.warmer = warmer;
        this.config = config;
        this.skinResolver = skinResolver;
        this.logger = logger;
    }

    public boolean isReady() {
        return warmer == null || warmer.isReady();
    }

    /**
     * 渲染玩家背包界面（原版 InventoryScreen 布局，含 3D 玩家模型与装备）
     * */
    public MvgRenderResult renderInventory(PlayerSnapshotPayload payload) throws MvgApiException {
        ensureReady();
        JsonObject data = new JsonObject();
        data.addProperty("title", title(config.inventoryTitle, payload.getPlayerName()));
        data.add("skin", skin(payload));
        addIfPresent(data, "slots", payload.getSlots());
        addIfPresent(data, "armor", payload.getArmor());
        addIfPresent(data, "mainHand", payload.getMainHand());
        addIfPresent(data, "offhand", payload.getOffHand());
        return client.render(client.buildRenderPayload(TYPE_PLAYER_INVENTORY, data, config.scaleInventory));
    }

    /**
     * 渲染末影箱容器
     * */
    public MvgRenderResult renderEnderChest(PlayerSnapshotPayload payload) throws MvgApiException {
        ensureReady();
        JsonObject data = new JsonObject();
        data.addProperty("title", title(config.enderChestTitle, payload.getPlayerName()));
        data.addProperty("rows", ENDER_CHEST_ROWS);
        data.addProperty("columns", ENDER_CHEST_COLUMNS);
        addIfPresent(data, "slots", payload.getSlots());
        return client.render(client.buildRenderPayload(TYPE_ENDER_CHEST, data, config.scaleEnderChest));
    }

    /**
     * 渲染手持物 tooltip
     * <p>
     * tooltip 的文本行必须在采集端（玩家所在子服）生成 —— 只有那里能访问到真实的 {@link org.bukkit.inventory.ItemStack}，
     * 因此这里直接接收已经组装好的 data 对象。
     *
     * @param data 采集端生成的 tooltip 数据，物品为空时传入 {@code null}（返回 {@code null} 由调用方给出文字提示）
     * */
    public MvgRenderResult renderHandTooltip(JsonObject data) throws MvgApiException {
        if (data == null || data.size() == 0) {
            return null;
        }
        ensureReady();
        return client.render(client.buildRenderPayload(TYPE_TOOLTIP, data, config.scaleTooltip));
    }

    /**
     * 渲染服务器在线列表（原版 TAB 风格）
     * */
    public MvgRenderResult renderPlayerList(List<OnlinePlayerEntry> players) throws MvgApiException {
        ensureReady();
        JsonObject data = new JsonObject();
        Set<String> servers = new LinkedHashSet<>();
        JsonArray array = new JsonArray();
        for (OnlinePlayerEntry entry : players) {
            // mvg 的 playerlist 要求每个玩家都必须带合法头像，缺一个整张图就会被拒绝
            if (entry.getTexture() == null || entry.getTexture().isEmpty()) {
                logger.debug("[Render] 跳过缺少头像的玩家: " + entry.getName());
                continue;
            }
            if (entry.getServerName() != null && !entry.getServerName().isEmpty()) {
                servers.add(entry.getServerName());
            }
            JsonObject player = new JsonObject();
            player.addProperty("name", entry.getName());
            player.addProperty("display",
                    entry.getDisplay() == null || entry.getDisplay().isEmpty() ? entry.getName() : entry.getDisplay());
            if (entry.getUuid() != null && !entry.getUuid().isEmpty()) {
                player.addProperty("uuid", entry.getUuid());
            }
            player.addProperty("texture", entry.getTexture() == null ? "" : entry.getTexture());
            player.addProperty("ping", entry.getPing());
            if (entry.getGameMode() != null && !entry.getGameMode().isEmpty()) {
                player.addProperty("gameMode", entry.getGameMode());
            }
            array.add(player);
        }
        data.add("players", array);
        // 数量以实际绘制的人数为准（可能有个别玩家因缺少头像被过滤）
        String total = Integer.toString(array.size());
        String header = config.playerListHeader
                .replace("%total%", total)
                .replace("%servers%", String.join(", ", servers));
        String footer = config.playerListFooter
                .replace("%total%", total)
                .replace("%servers%", String.join(", ", servers));
        if (!header.isEmpty()) {
            data.addProperty("header", header);
        }
        if (!footer.isEmpty()) {
            data.addProperty("footer", footer);
        }
        if (config.playerListColumns > 0) {
            data.addProperty("columns", config.playerListColumns);
        }
        return client.render(client.buildRenderPayload(TYPE_PLAYER_LIST, data, config.scalePlayerList));
    }

    private JsonObject skin(PlayerSnapshotPayload payload) {
        String texture = payload.getSkinDataUri();
        String model = payload.getSkinModel();
        if (texture == null || texture.isEmpty()) {
            // 采集端没拿到皮肤时不能下发空 texture：mvg 会直接 422，
            // 这里退回内置默认皮肤并如实记录，保证背包界面仍然可渲染
            SkinData fallback = skinResolver == null ? null : skinResolver.getDefault(model);
            if (fallback != null && fallback.getDataUri() != null && !fallback.getDataUri().isEmpty()) {
                texture = fallback.getDataUri();
                model = fallback.getModel();
                logger.warn("[Render] 玩家 " + payload.getPlayerName() + " 缺少皮肤数据，已使用内置默认皮肤");
            } else {
                texture = "";
                logger.warn("[Render] 玩家 " + payload.getPlayerName() + " 缺少皮肤数据，且内置默认皮肤不可用");
            }
        }
        JsonObject skin = new JsonObject();
        skin.addProperty("texture", texture);
        skin.addProperty("model", model == null ? config.skinFallbackModel : model);
        // 原版外层（帽子/外套/袖子/裤腿）
        skin.addProperty("outerLayer", true);
        return skin;
    }

    private String title(String template, String playerName) {
        String name = playerName == null ? "" : playerName;
        return template.replace("%player%", name);
    }

    private static void addIfPresent(JsonObject data, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof JsonArray) {
            JsonArray array = (JsonArray) value;
            if (array.size() == 0) {
                return;
            }
            data.add(key, array);
            return;
        }
        if (value instanceof JsonObject) {
            data.add(key, (JsonObject) value);
            return;
        }
        throw new IllegalArgumentException("unsupported render data type: " + value.getClass());
    }

    private void ensureReady() throws MvgApiException {
        if (warmer == null || warmer.isReady()) {
            return;
        }
        throw new MvgApiException(-1, "warming_up", warmer.describeState());
    }

}
