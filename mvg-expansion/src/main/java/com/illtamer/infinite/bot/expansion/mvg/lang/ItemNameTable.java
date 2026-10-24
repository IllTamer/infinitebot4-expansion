package com.illtamer.infinite.bot.expansion.mvg.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * 简体中文名称表
 * <p>
 * 数据来源为 Minecraft 官方资源索引中的 {@code assets/minecraft/lang/zh_cn.json}，
 * 构建期提取物品/方块/附魔/属性等条目并以原始语言键保存。
 * mvg 侧生成 tooltip 时使用的是其内置的 en_us，因此附属需要自行提供中文文案。
 * */
public class ItemNameTable {

    private static final String ITEM_PREFIX = "item.minecraft.";
    private static final String BLOCK_PREFIX = "block.minecraft.";
    private static final String ENCHANTMENT_PREFIX = "enchantment.minecraft.";
    private static final String ENCHANTMENT_LEVEL_PREFIX = "enchantment.level.";
    private static final String ATTRIBUTE_PREFIX = "attribute.name.";
    private static final String ATTRIBUTE_MODIFIER_PREFIX = "attribute.modifier.";
    private static final String ITEM_MODIFIER_PREFIX = "item.modifiers.";

    private final Map<String, String> table;

    private ItemNameTable(Map<String, String> table) {
        this.table = table;
    }

    public static ItemNameTable empty() {
        return new ItemNameTable(Collections.emptyMap());
    }

    /**
     * 从 json 资源流加载名称表
     * */
    public static ItemNameTable load(InputStream input) {
        if (input == null) {
            return empty();
        }
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            Map<String, String> parsed = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            return parsed == null ? empty() : new ItemNameTable(parsed);
        } catch (Exception e) {
            return empty();
        }
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public int size() {
        return table.size();
    }

    public String get(String key) {
        return table.get(key);
    }

    /**
     * 物品名：优先 {@code item.minecraft.<path>}，回退 {@code block.minecraft.<path>}
     * <p>
     * 与 mvg 内置 en_us 的解析顺序保持一致
     * */
    public String itemName(String path) {
        String name = table.get(ITEM_PREFIX + path);
        if (name != null) {
            return name;
        }
        return table.get(BLOCK_PREFIX + path);
    }

    public String enchantmentName(String path) {
        return table.get(ENCHANTMENT_PREFIX + path);
    }

    /**
     * 附魔等级文本（罗马数字），超出语言文件范围时回退阿拉伯数字
     * */
    public String enchantmentLevel(int level) {
        String text = table.get(ENCHANTMENT_LEVEL_PREFIX + level);
        return text != null ? text : Integer.toString(level);
    }

    public String attributeName(String path) {
        return table.get(ATTRIBUTE_PREFIX + path);
    }

    /**
     * 属性修饰符文案模板，形如 {@code attribute.modifier.plus.0 -> "+%s %s"}
     * */
    public String attributeModifierPattern(String kind, int operationIndex) {
        String value = table.get(ATTRIBUTE_MODIFIER_PREFIX + kind + "." + operationIndex);
        return value != null ? value : "%s %s";
    }

    /**
     * 属性槽位表头，形如 {@code item.modifiers.mainhand -> "在主手时："}
     * */
    public String itemModifierHeader(String slot) {
        return table.get(ITEM_MODIFIER_PREFIX + slot);
    }

    public String misc(String key) {
        return table.get(key);
    }

}
