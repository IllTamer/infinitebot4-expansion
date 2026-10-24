package com.illtamer.infinite.bot.expansion.mvg.render;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.expansion.mvg.config.MvgConfig;
import com.illtamer.infinite.bot.expansion.mvg.lang.ItemNameTable;
import com.illtamer.infinite.bot.expansion.mvg.util.LegacyText;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 手持物品 tooltip 构造器
 * <p>
 * mvg 生成 tooltip 时，物品名可以用 {@code item.name} 覆盖为中文，
 * 但附魔行、属性行、耐久行都由 mvg 用它内置的 en_us 生成，外部无法干预。
 * 因此中文模式（{@code render.tooltip-mode: chinese}）下，附属自行生成全部文本行 ——
 * 当前接口已经没有独立的 {@code lines} 字段，正文只能写进 {@code item.lore}。
 * {@code vanilla} 模式则完全交给 mvg 按原版逻辑渲染。
 * <p>
 * 行顺序按原版 {@code ItemStack#getTooltipLines}（与游戏内截图一致）：
 * 标题 → 附魔 → lore（描述/状态等）→ 属性修饰符 → 单值组件。
 * */
public class TooltipBuilder {

    /**
     * 与原版 {@code ATTRIBUTE_MODIFIER_FORMAT} 一致，符号固定不随 JVM 默认区域变化
     * */
    private static final DecimalFormat DECIMAL =
            new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.ROOT));

    /**
     * mvg 对 {@code item.lore} 的限制：最多 128 行、每行 1024 字符
     * */
    private static final int MAX_LORE_LINES = 128;
    private static final int MAX_LORE_LENGTH = 1024;

    /**
     * 原版会把基础值与 {@code add_value} 修饰符合并成合计值显示的属性
     * <p>
     * 例如钻石剑显示「8 攻击伤害」「1.6 攻击速度」，而不是「+7」「-2.4」。
     * */
    private static final List<String> MERGED_ATTRIBUTES = List.of("attack_damage", "attack_speed");

    private final ItemNameTable names;
    private final MvgConfig config;
    private final Logger logger;

    public TooltipBuilder(ItemNameTable names, MvgConfig config, Logger logger) {
        this.names = names;
        this.config = config;
        this.logger = logger;
    }

    /**
     * 构造 mvg tooltip 类型的 data 对象
     * */
    public JsonObject build(ItemStack stack) {
        return build(null, stack);
    }

    /**
     * 构造 mvg tooltip 类型的 data 对象
     *
     * @param player 物品所属玩家，用于读取属性基础值（合并显示「8 攻击伤害」这类行）；
     *               为 {@code null} 时退化为逐条 {@code +}/{@code -} 显示
     * */
    public JsonObject build(Player player, ItemStack stack) {
        if (ItemStackCodec.isEmpty(stack)) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        boolean chinese = config.useChineseTooltip();
        JsonObject data = new JsonObject();
        data.add("item", ItemStackCodec.toItem(stack, !chinese));
        if (chinese) {
            data.addProperty("title", displayName(stack, meta));
            JsonArray lines = buildChineseLines(player, stack, meta);
            if (lines.size() > 0) {
                // 接口变更：tooltip 没有独立的 lines 字段，正文只能通过 item.lore 传给 mvg
                data.getAsJsonObject("item").add("lore", toLore(lines));
            }
        } else {
            // vanilla：物品名走 item.name（中文），其余行交给 mvg 按原版顺序生成
            String name = plainDisplayName(stack, meta);
            if (!name.isEmpty()) {
                JsonObject item = data.getAsJsonObject("item");
                item.addProperty("name", name);
            }
            if (meta != null && meta.hasLore()) {
                JsonArray lore = new JsonArray();
                for (String line : meta.getLore()) {
                    lore.add(line == null ? "" : line);
                }
                if (lore.size() > 0) {
                    data.getAsJsonObject("item").add("lore", toLore(lore));
                }
            }
        }
        if (config.tooltipIcon) {
            data.addProperty("icon", true);
        }
        return data;
    }

    /**
     * 中文模式下自行拼装全部行，顺序与原版 {@code ItemStack#getTooltipLines} 对齐：
     * 附魔 → lore（描述/状态）→ 属性修饰符 → 单值组件
     * */
    private JsonArray buildChineseLines(Player player, ItemStack stack, ItemMeta meta) {
        JsonArray enchantments = new JsonArray();
        JsonArray lore = new JsonArray();
        JsonArray attributes = new JsonArray();
        JsonArray components = new JsonArray();
        if (meta != null) {
            addEnchantments(enchantments, meta);
            addLore(lore, meta);
            addAttributeModifiers(player, stack, meta, attributes);
            addSingleValueComponents(components, meta);
        }
        return assembleLines(enchantments, lore, attributes, components);
    }

    /**
     * 按原版顺序拼接各段行
     * <p>
     * 纯 JsonArray 装配，不依赖 Bukkit 运行期对象，便于离线沙箱断言顺序
     * （{@code MvgSandboxMain} 直接调用本方法）。
     * */
    public static JsonArray assembleLines(JsonArray enchantments, JsonArray lore,
                                          JsonArray attributes, JsonArray components) {
        JsonArray lines = new JsonArray();
        append(lines, enchantments);
        append(lines, lore);
        append(lines, attributes);
        append(lines, components);
        return lines;
    }

    private static void append(JsonArray target, JsonArray source) {
        if (source == null) {
            return;
        }
        for (int index = 0; index < source.size(); index++) {
            target.add(source.get(index).getAsString());
        }
    }

    /**
     * 中文行 → mvg 的 {@code item.lore}
     * <p>
     * mvg 限制最多 128 行、每行 1024 字符，超出会被入口以 400 拒绝，
     * 因此这里主动截断并记录一次，保证 tooltip 仍然能渲染出来。
     * */
    private JsonArray toLore(JsonArray lines) {
        JsonArray lore = new JsonArray();
        int limit = Math.min(lines.size(), MAX_LORE_LINES);
        boolean truncated = lines.size() > MAX_LORE_LINES;
        for (int index = 0; index < limit; index++) {
            String line = lines.get(index).getAsString();
            // mvg 按码点计长度，这里也按码点截断，避免把代理对切成半个字符
            if (line.codePointCount(0, line.length()) > MAX_LORE_LENGTH) {
                line = line.substring(0, line.offsetByCodePoints(0, MAX_LORE_LENGTH));
                truncated = true;
            }
            lore.add(line);
        }
        if (truncated && logger != null) {
            logger.debug("[Render] tooltip 文本超过 mvg 限制，已截断为 " + limit + " 行");
        }
        return lore;
    }

    private void addLore(JsonArray lines, ItemMeta meta) {
        if (!meta.hasLore()) {
            return;
        }
        for (String raw : meta.getLore()) {
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            lines.add(LegacyText.containsStyle(raw) ? raw : LegacyText.DARK_PURPLE + LegacyText.ITALIC + raw);
        }
    }

    private void addEnchantments(JsonArray lines, ItemMeta meta) {
        if (!meta.hasEnchants()) {
            return;
        }
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            String path = entry.getKey().getKey().getKey();
            String name = names.enchantmentName(path);
            if (name == null) {
                name = path;
            }
            int level = entry.getValue();
            StringBuilder builder = new StringBuilder();
            builder.append(LegacyText.GRAY).append(name);
            if (level > 1) {
                builder.append(' ').append(names.enchantmentLevel(level));
            }
            lines.add(builder.toString());
        }
    }

    /**
     * 属性修饰符行
     * <p>
     * 1.21 的物品默认属性写在物品定义里，{@code ItemMeta} 上没有修饰符（例如普通钻石剑），
     * 因此 meta 为空修饰符时回落到物品类型的默认修饰符，否则属性区块会整块消失。
     * */
    private void addAttributeModifiers(Player player, ItemStack stack, ItemMeta meta, JsonArray lines) {
        Multimap<Attribute, AttributeModifier> modifiers;
        if (meta.hasAttributeModifiers()) {
            modifiers = meta.getAttributeModifiers();
        } else {
            modifiers = defaultAttributeModifiers(stack);
        }
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            String path = entry.getKey().getKey().getKey();
            String attributeName = names.attributeName(path);
            if (attributeName == null) {
                attributeName = path;
            }
            AttributeModifier modifier = entry.getValue();
            String slot = ItemStackCodec.slotGroupName(modifier.getSlotGroup());
            String text = attributeLine(path, attributeName, modifier.getAmount(),
                    ItemStackCodec.operationName(modifier.getOperation()),
                    baseValue(player, entry.getKey()), names);
            grouped.computeIfAbsent(slot, key -> new ArrayList<>()).add(LegacyText.BLUE + text);
        }
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            String header = names.itemModifierHeader(entry.getKey());
            if (header != null && !header.isEmpty()) {
                lines.add(LegacyText.GRAY + header);
            }
            for (String line : entry.getValue()) {
                lines.add(line);
            }
        }
    }

    /**
     * 单个属性修饰符 → 一行文本
     * <p>
     * 与原版保持一致：{@code attack_damage}/{@code attack_speed} 这类属性会把玩家基础值
     * 与 {@code add_value} 合并成合计值（「8 攻击伤害」），其它属性逐条显示 {@code +}/{@code -}
     * （「+0.75 横扫伤害比率」）；倍率类修饰符按百分比显示。
     * <p>
     * 纯函数，便于离线沙箱断言（{@code MvgSandboxMain} 直接调用）。
     *
     * @param path 属性键路径，如 {@code attack_damage}
     * @param attributeName 已本地化的属性名，如「攻击伤害」
     * @param baseValue 玩家该属性的基础值；{@link Double#NaN} 表示未知（退化为逐条显示）
     * */
    public static String attributeLine(String path, String attributeName, double amount, String operation,
                                       double baseValue, ItemNameTable names) {
        int operationIndex = operationIndex(operation);
        String kind;
        double shown;
        if (operationIndex != 0) {
            kind = "equals";
            shown = amount * 100D;
        } else if (MERGED_ATTRIBUTES.contains(path) && !Double.isNaN(baseValue)) {
            kind = "equals";
            shown = baseValue + amount;
        } else if (amount >= 0) {
            kind = "plus";
            shown = amount;
        } else {
            kind = "take";
            shown = -amount;
        }
        String pattern = names.attributeModifierPattern(kind, operationIndex);
        return String.format(pattern, DECIMAL.format(shown), attributeName);
    }

    /**
     * 物品类型自带的默认属性修饰符（1.21 的物品定义）
     * */
    private static Multimap<Attribute, AttributeModifier> defaultAttributeModifiers(ItemStack stack) {
        try {
            org.bukkit.inventory.ItemType type = stack.getType().asItemType();
            if (type != null) {
                Multimap<Attribute, AttributeModifier> modifiers = type.getDefaultAttributeModifiers();
                if (modifiers != null) {
                    return modifiers;
                }
            }
        } catch (Throwable ignored) {
            // 老版本/非物品类型时退到 Material 的兼容接口
        }
        try {
            @SuppressWarnings("deprecation")
            Multimap<Attribute, AttributeModifier> modifiers = stack.getType().getDefaultAttributeModifiers();
            return modifiers == null ? ImmutableMultimap.of() : modifiers;
        } catch (Throwable ignored) {
            return ImmutableMultimap.of();
        }
    }

    /**
     * 玩家属性的基础值（用于合并显示）；不可用时返回 {@link Double#NaN}
     * */
    private static double baseValue(Player player, Attribute attribute) {
        if (player == null || attribute == null) {
            return Double.NaN;
        }
        try {
            AttributeInstance instance = player.getAttribute(attribute);
            return instance == null ? Double.NaN : instance.getDefaultValue();
        } catch (Throwable ignored) {
            return Double.NaN;
        }
    }

    private void addSingleValueComponents(JsonArray lines, ItemMeta meta) {
        if (meta.isUnbreakable()) {
            String text = names.misc("item.unbreakable");
            lines.add(LegacyText.BLUE + (text == null ? "Unbreakable" : text));
        }
        if (hasDyedColor(meta)) {
            String text = names.misc("item.dyed");
            lines.add(LegacyText.GRAY + (text == null ? "Dyed" : text));
        }
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            if (damageable.hasDamage() && damageable.hasMaxDamage()) {
                int maxDamage = damageable.getMaxDamage();
                int remain = maxDamage - damageable.getDamage();
                String pattern = names.misc("item.durability");
                if (pattern == null) {
                    pattern = "Durability: %s / %s";
                }
                lines.add(LegacyText.GRAY + String.format(pattern, DECIMAL.format(remain), DECIMAL.format(maxDamage)));
            }
        }
    }

    private static boolean hasDyedColor(ItemMeta meta) {
        return meta instanceof LeatherArmorMeta
                || meta instanceof PotionMeta
                || meta instanceof MapMeta;
    }

    private static int operationIndex(String operation) {
        switch (operation) {
            case "add_multiplied_base":
                return 1;
            case "add_multiplied_total":
                return 2;
            default:
                return 0;
        }
    }

    /**
     * 用于 tooltip 标题的显示名
     * <p>
     * 自定义名（{@code ItemMeta#getDisplayName()}）已是 legacy 文本，原样保留；
     * 否则用中文名并按稀有度着色
     * */
    private String displayName(ItemStack stack, ItemMeta meta) {
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        String name = plainDisplayName(stack, meta);
        return LegacyText.withRarity(name, meta == null ? null : meta.getRarity());
    }

    /**
     * 不含颜色码的物品名，供 vanilla 模式的 {@code item.name} 使用（mvg 会按稀有度补色）
     * */
    private String plainDisplayName(ItemStack stack, ItemMeta meta) {
        if (meta != null && meta.hasDisplayName()) {
            return LegacyText.strip(meta.getDisplayName());
        }
        String path = stack.getType().getKey().getKey();
        String name = names.itemName(path);
        return name != null ? name : path;
    }

}
