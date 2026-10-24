package com.illtamer.infinite.bot.expansion.mvg.render;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义物品 {@code customData} 的下发策略
 * <p>
 * mvg 只认物品级字段 {@code item.customData}（配合基础材料去查 ItemsAdder 物品索引），
 * {@code item.customModelData}、{@code components["minecraft:custom_model_data"]}
 * 与 snake_case 写法都会被静默忽略，因此下发时只写 {@code customData} 这一个字段。
 * <p>
 * 是否下发由配置 {@code render.custom-data} 决定：
 * <ul>
 *     <li>{@code always}（默认）：只要物品带正整数 {@code custom_model_data} 就下发。
 *     索引键是「基础材料 + 数值」，mvg 查不到就是原版材质（并记一条 WARN），
 *     所以这里不需要也不应该先替 mvg 判断"是不是自定义物品"；</li>
 *     <li>{@code itemsadder}：只下发 {@link ItemsAdderHook} 判定为 ItemsAdder 托管的物品。
 *     <b>不建议在混合服使用</b> —— 大量插件（钓鱼、MMO 武器等）只是复用 ItemsAdder 的
 *     {@code custom_model_data} 数值来吃它的贴图，物品本身并不由 ItemsAdder 创建，
 *     {@code ItemsAdder.isCustomItem()} 对这些物品一律返回 {@code false}，
 *     结果就是客户端看得见自定义贴图、图片里却是原版材质。</li>
 * </ul>
 * {@code itemsadder} 模式被跳过时按「材料 + 数值」打一次性日志：线上"报文里没有 customData"时，
 * 能立刻看出是识别环节跳过，而不是报文格式问题。
 * */
public final class CustomDataSupport {

    /** 只下发 ItemsAdder API 判定为自定义物品的物品（严格模式，混合服会漏掉复用数值的物品） */
    public static final String MODE_ITEMSADDER = "itemsadder";
    /** 只要物品带正整数 {@code custom_model_data} 就下发（默认） */
    public static final String MODE_ALWAYS = "always";

    /**
     * 跳过诊断日志的条目上限，避免带自定义模型数据的物品很多时无限增长
     * */
    private static final int SKIP_LOG_LIMIT = 256;

    private static volatile boolean checked;
    private static volatile boolean enabled = true;
    private static volatile String mode = MODE_ALWAYS;
    private static Logger logger;

    private static final Map<String, Boolean> SKIPPED = new ConcurrentHashMap<>();

    private CustomDataSupport() {}

    /**
     * 在附属 onEnable 中调用
     *
     * @param hookEnabled 配置开关 {@code render.itemsadder}，关闭后完全不下发 customData
     * @param customDataMode 配置 {@code render.custom-data}（{@code always} 默认 / {@code itemsadder}）
     * */
    public static void init(Logger expansionLogger, boolean hookEnabled, String customDataMode) {
        logger = expansionLogger;
        checked = true;
        enabled = hookEnabled;
        mode = MODE_ITEMSADDER.equalsIgnoreCase(customDataMode) ? MODE_ITEMSADDER : MODE_ALWAYS;
        SKIPPED.clear();
    }

    public static String mode() {
        return mode;
    }

    /**
     * 是否一律下发（不看 ItemsAdder 识别结果）
     * */
    public static boolean isAlways() {
        return MODE_ALWAYS.equals(mode);
    }

    /**
     * 把自定义模型数据写进 mvg 的物品对象
     * <p>
     * 检测逻辑留在 {@link ItemsAdderHook}，这里只做「写不写 {@code customData}」的决策，
     * 决策本身是纯函数（{@link ItemStackCodec#writeCustomData}），便于离线沙箱断言。
     * <p>
     * {@code always} 模式下不查 ItemsAdder：那次 API 调用对"复用 ItemsAdder 数值但不由它托管"
     * 的物品只会返回 {@code false}，查了反而把它们挡在门外，还平白多一次开销。
     *
     * @return 是否下发了 {@code customData}
     * */
    public static boolean apply(JsonObject item, ItemStack stack, Integer customModelData) {
        if (item == null || customModelData == null || customModelData <= 0) {
            return false;
        }
        boolean recognized = !isAlways() && ItemsAdderHook.isCustomItem(stack);
        boolean sent = ItemStackCodec.writeCustomData(item, customModelData, recognized, mode, enabled);
        if (!sent && enabled && !isAlways()) {
            noteSkipped(stack, customModelData, recognized);
        }
        return sent;
    }

    private static void noteSkipped(ItemStack stack, int value, boolean recognized) {
        if (logger == null || !checked || !enabled) {
            return;
        }
        String material = stack == null || stack.getType() == null
                ? "unknown" : stack.getType().getKey().toString();
        String key = material + "#" + value;
        if (SKIPPED.size() >= SKIP_LOG_LIMIT || SKIPPED.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }
        String reason = recognized
                ? "但配置不允许下发 customData"
                : "但它不由 ItemsAdder 托管（ItemsAdder 未接入、物品未加载完，"
                + "或只是钓鱼/MMO 等插件复用了这个 custom_model_data 数值）";
        logger.info(String.format("[Items] %s 携带自定义模型数据，%s，itemsadder 模式已跳过 customData；"
                + "如需下发请把 render.custom-data 设为 always（默认值）", key, reason));
    }

}
