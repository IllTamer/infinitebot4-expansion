package com.illtamer.infinite.bot.expansion.mvg.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bukkit {@link ItemStack} → mvg 渲染接口的 Item 对象
 * <p>
 * mvg 侧只需要物品 id / 数量 / 若干原版组件即可还原图标，
 * 因此这里只提取 mvg 真正消费的字段（详见 mvg 的 openapi 文档与渲染实现）。
 * */
public final class ItemStackCodec {

    /** mvg 的自定义物品索引键，写在物品级（不能写成 {@code customModelData}，也不能塞进 {@code components}） */
    public static final String KEY_CUSTOM_DATA = "customData";
    public static final String KEY_RARITY = "minecraft:rarity";
    public static final String KEY_UNBREAKABLE = "minecraft:unbreakable";
    public static final String KEY_DYED_COLOR = "minecraft:dyed_color";
    public static final String KEY_ATTRIBUTE_MODIFIERS = "minecraft:attribute_modifiers";

    private ItemStackCodec() {}

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0;
    }

    /**
     * 把物品转换为 mvg 的 Item 对象
     *
     * @param includeEnchantments 是否写入附魔字段
     *        <p>
     *        mvg 只要读到附魔就会用它内置的 en_us 追加附魔行，
     *        因此中文 tooltip 模式下必须关闭，否则会出现中英重复
     * */
    public static JsonObject toItem(ItemStack stack, boolean includeEnchantments) {
        JsonObject item = new JsonObject();
        if (isEmpty(stack)) {
            return item;
        }
        item.addProperty("id", stack.getType().getKey().toString());
        item.addProperty("count", stack.getAmount());

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return item;
        }
        Integer customModelData = customModelData(meta);
        if (customModelData != null) {
            // 只写物品级 customData（mvg 的索引键）；是否下发由 render.custom-data 与 ItemsAdder 识别结果共同决定
            CustomDataSupport.apply(item, stack, customModelData);
        }
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            if (damageable.hasDamage()) {
                item.addProperty("damage", damageable.getDamage());
            }
            if (damageable.hasMaxDamage()) {
                item.addProperty("maxDamage", damageable.getMaxDamage());
            }
        }
        if (meta.hasRarity()) {
            JsonObject components = components(item);
            components.addProperty(KEY_RARITY, meta.getRarity().name().toLowerCase(Locale.ROOT));
        }
        if (meta.isUnbreakable()) {
            JsonObject components = components(item);
            components.add(KEY_UNBREAKABLE, new JsonObject());
        }
        extractDyedColor(meta, item);
        extractAttributeModifiers(meta, item);
        if (includeEnchantments && meta.hasEnchants()) {
            JsonArray enchantments = new JsonArray();
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchantments.add(entry.getKey().getKey() + " " + entry.getValue());
            }
            item.add("enchantments", enchantments);
        }
        return item;
    }

    /**
     * 把自定义模型数据写进 mvg 的物品对象
     * <p>
     * 只写物品级 {@link #KEY_CUSTOM_DATA}（{@code customData}）：它是 mvg 查自定义物品索引的键。
     * 不写 {@code customModelData} —— mvg 不读该字段，写进去只会让报文看起来"有值却没生效"；
     * 也不写进 {@code components["minecraft:custom_model_data"]}，那里同样会被静默忽略。
     * <p>
     * 值为 {@code 0} 等同于"没有自定义模型数据"（原版组件语义），负值会被 mvg 直接拒绝，
     * 两者都不下发，免得给索引塞 {@code MATERIAL#0} 这种必然未命中的请求。
     * <p>
     * 纯字段装配，不读 ItemStack、不读配置状态，便于离线沙箱直接断言各分支。
     *
     * @param enabled 配置总开关 {@code render.itemsadder}
     * @param mode {@link CustomDataSupport#MODE_ITEMSADDER} 或 {@link CustomDataSupport#MODE_ALWAYS}
     * @return 是否写入了 {@code customData}
     * */
    public static boolean writeCustomData(JsonObject item, Integer customData,
                                          boolean customItem, String mode, boolean enabled) {
        if (!enabled || item == null || customData == null || customData <= 0) {
            return false;
        }
        boolean always = CustomDataSupport.MODE_ALWAYS.equalsIgnoreCase(mode);
        if (!always && !customItem) {
            return false;
        }
        item.addProperty(KEY_CUSTOM_DATA, customData);
        return true;
    }

    /**
     * 把物品转换为 mvg 的容器槽位对象
     * */
    public static JsonObject toSlot(int slot, ItemStack stack) {
        JsonObject object = new JsonObject();
        object.addProperty("slot", slot);
        object.add("item", toItem(stack, false));
        return object;
    }

    /**
     * 读取物品的 custom_model_data 数值
     * <p>
     * 1.21.11 的 custom_model_data 是一个组件（{@code floats / flags / strings / colors}），
     * ItemsAdder 把物品的数值写在 {@code floats[0]}。mvg 的物品索引以整数为键，
     * 因此只接受整数值；只有字符串或颜色时视为没有自定义模型数据。
     *
     * @return 整数值；不存在或不是整数时返回 {@code null}
     * */
    public static Integer customModelData(ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        if (meta.hasCustomModelDataComponent()) {
            List<Float> floats = meta.getCustomModelDataComponent().getFloats();
            if (floats != null && !floats.isEmpty()) {
                float value = floats.get(0);
                int rounded = Math.round(value);
                if (Math.abs(value - rounded) <= 1e-4F) {
                    return rounded;
                }
            }
            return null;
        }
        // 兼容只写入整数的旧实现
        return meta.hasCustomModelData() ? meta.getCustomModelData() : null;
    }

    /**
     * 提取染色组件（皮革护甲 / 药水 / 地图 / 旗帜等）
     * */
    private static void extractDyedColor(ItemMeta meta, JsonObject item) {
        Color color = null;
        if (meta instanceof LeatherArmorMeta) {
            color = ((LeatherArmorMeta) meta).getColor();
        } else if (meta instanceof PotionMeta) {
            color = ((PotionMeta) meta).getColor();
        } else if (meta instanceof MapMeta) {
            color = ((MapMeta) meta).getColor();
        }
        if (color == null) {
            return;
        }
        JsonObject components = components(item);
        components.addProperty(KEY_DYED_COLOR, color.asRGB());
    }

    /**
     * 提取属性修饰符，字段名与 mvg 期望的 {@code {type, amount, operation, slot}} 对齐
     * */
    private static void extractAttributeModifiers(ItemMeta meta, JsonObject item) {
        if (!meta.hasAttributeModifiers()) {
            return;
        }
        JsonArray modifiers = new JsonArray();
        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : meta.getAttributeModifiers().asMap().entrySet()) {
            String attributeKey = entry.getKey().getKey().toString();
            for (AttributeModifier modifier : entry.getValue()) {
                JsonObject object = new JsonObject();
                object.addProperty("type", attributeKey);
                object.addProperty("amount", modifier.getAmount());
                object.addProperty("operation", operationName(modifier.getOperation()));
                object.addProperty("slot", slotGroupName(modifier.getSlotGroup()));
                modifiers.add(object);
            }
        }
        if (modifiers.size() == 0) {
            return;
        }
        JsonObject components = components(item);
        JsonObject attributeModifiers = new JsonObject();
        attributeModifiers.add("modifiers", modifiers);
        components.add(KEY_ATTRIBUTE_MODIFIERS, attributeModifiers);
    }

    private static JsonObject components(JsonObject item) {
        JsonObject components = item.getAsJsonObject("components");
        if (components == null) {
            components = new JsonObject();
            item.add("components", components);
        }
        return components;
    }

    /**
     * 属性修饰符运算类型 → 原版组件枚举名
     * <p>
     * Paper 保留了两套枚举字面量（旧名 ADD_NUMBER/ADD_SCALAR/MULTIPLY_SCALAR_1 与新名），
     * 这里同时兼容，未知值回退 {@code add_value}
     * */
    static String operationName(AttributeModifier.Operation operation) {
        if (operation == null) {
            return "add_value";
        }
        switch (operation.name()) {
            case "ADD_SCALAR":
            case "ADD_MULTIPLIED_BASE":
                return "add_multiplied_base";
            case "MULTIPLY_SCALAR_1":
            case "ADD_MULTIPLIED_TOTAL":
                return "add_multiplied_total";
            default:
                return "add_value";
        }
    }

    /**
     * 装备槽位组 → 原版槽位名
     * */
    static String slotGroupName(EquipmentSlotGroup group) {
        if (group == null) {
            return "any";
        }
        if (group == EquipmentSlotGroup.MAINHAND) {
            return "mainhand";
        }
        if (group == EquipmentSlotGroup.OFFHAND) {
            return "offhand";
        }
        if (group == EquipmentSlotGroup.HAND) {
            return "hand";
        }
        if (group == EquipmentSlotGroup.HEAD) {
            return "head";
        }
        if (group == EquipmentSlotGroup.CHEST) {
            return "chest";
        }
        if (group == EquipmentSlotGroup.LEGS) {
            return "legs";
        }
        if (group == EquipmentSlotGroup.FEET) {
            return "feet";
        }
        if (group == EquipmentSlotGroup.ARMOR) {
            return "armor";
        }
        if (group == EquipmentSlotGroup.BODY) {
            return "body";
        }
        if (group == EquipmentSlotGroup.SADDLE) {
            return "saddle";
        }
        return "any";
    }

}
