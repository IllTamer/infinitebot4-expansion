package com.illtamer.infinite.bot.expansion.mvg.render;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

/**
 * ItemsAdder 软依赖钩子
 * <p>
 * <b>这是全项目唯一引用 {@code dev.lone.itemsadder.*} 的类。</b>
 * 附属没有安装 ItemsAdder 时，{@link #init(Logger)} 不会触碰任何 ItemsAdder 类型；
 * 即便插件存在但物品还没加载完，{@link #isCustomItem(ItemStack)} 也会捕获全部 {@link Throwable}
 * 并返回 {@code false}。
 * <p>
 * 只负责"这个物品是不是 ItemsAdder 托管的自定义物品"，供
 * {@code render.custom-data=itemsadder} 这一严格模式使用；默认的 {@code always} 模式不查这里 ——
 * 大量物品（钓鱼、MMO 武器等）只是复用 ItemsAdder 的 {@code custom_model_data} 数值来吃它的贴图，
 * 并不由 ItemsAdder 创建，{@link #isCustomItem(ItemStack)} 对它们一律返回 {@code false}。
 * 数值由 {@link ItemStackCodec#customModelData(org.bukkit.inventory.meta.ItemMeta)} 从组件里读。
 * */
public final class ItemsAdderHook {

    private static final String PLUGIN_NAME = "ItemsAdder";

    private static volatile boolean checked;
    private static volatile boolean enabled;
    private static volatile boolean available;
    private static Logger logger;

    private ItemsAdderHook() {}

    /**
     * 在附属 onEnable 中调用（必须在 ItemsAdder 完成初始化之后）
     *
     * @param hookEnabled 配置开关 {@code render.itemsadder}，关闭后完全不下发 customData
     * */
    public static void init(Logger expansionLogger, boolean hookEnabled) {
        logger = expansionLogger;
        checked = true;
        enabled = hookEnabled;
        available = false;
        if (!hookEnabled) {
            logger.info("[Items] 已按配置关闭 ItemsAdder 自定义物品支持");
            return;
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (plugin == null || !plugin.isEnabled()) {
                logger.info("[Items] 未检测到 " + PLUGIN_NAME
                        + "（render.custom-data=itemsadder 时将因无法识别而不下发 customData）");
                return;
            }
            // 能链接到 API 就认为接入成功：ItemsAdder 的物品是异步加载的，
            // 这里 items 还没加载完也没关系，后续每次判断都会重新检查 areItemsLoaded()
            available = true;
            if (Api.isLoaded()) {
                logger.info("[Items] 已接入 " + PLUGIN_NAME + "（仅 render.custom-data=itemsadder 时用它做严格过滤）");
            } else {
                logger.info("[Items] " + PLUGIN_NAME + " 物品尚未加载完成，加载完成后自动生效");
            }
        } catch (Throwable e) {
            // NoClassDefFoundError / LinkageError / 版本不兼容等，全部降级处理
            logger.warn("[Items] 接入 " + PLUGIN_NAME + " 失败，自定义物品将按原版材质渲染", e);
        }
    }

    /**
     * ItemsAdder 是否可用（已配置启用、已安装且 API 可链接）
     * <p>
     * 物品是否加载完成由每次调用时的 {@code areItemsLoaded()} 单独判断，
     * 因此 ItemsAdder 异步加载完成后无需重新初始化本钩子。
     * */
    public static boolean isAvailable() {
        return checked && enabled && available;
    }

    /**
     * 判断物品是否为 ItemsAdder 自定义物品
     * <p>
     * 只在物品本身带有自定义模型数据时调用，避免对普通物品做多余的 NBT 解析。
     * */
    public static boolean isCustomItem(ItemStack stack) {
        if (!isAvailable() || stack == null) {
            return false;
        }
        try {
            return Api.isLoaded() && Api.isCustomItem(stack);
        } catch (Throwable e) {
            if (logger != null) {
                logger.debug("[Items] 判断自定义物品失败: " + stack.getType(), e);
            }
            return false;
        }
    }

    /**
     * 唯一引用 ItemsAdder API 的类
     * <p>
     * 未安装 ItemsAdder 时，加载本内部类会抛出 {@link NoClassDefFoundError}，
     * 由外层的 {@code try/catch (Throwable)} 接住，类加载器不会因为缺失符号而整体失败。
     * */
    private static final class Api {

        static boolean isLoaded() {
            return dev.lone.itemsadder.api.ItemsAdder.areItemsLoaded();
        }

        static boolean isCustomItem(ItemStack stack) {
            return dev.lone.itemsadder.api.ItemsAdder.isCustomItem(stack);
        }

    }

}
