package com.illtamer.infinite.bot.expansion.mvg.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Bukkit 与 mvg 之间的容器槽位映射
 * <p>
 * mvg 的 {@code playerInventory} 槽位沿用原版 {@code InventoryMenu} 玩家区编号：
 * {@code 0–26} 是背包、{@code 27–35} 是快捷栏；而 Bukkit 的
 * {@link PlayerInventory#getStorageContents()} 恰好相反（{@code 0–8} 快捷栏、{@code 9–35} 背包），
 * 两者必须显式换算，否则整张背包图会整体错位。
 * <p>
 * 同理，Bukkit 的 {@link PlayerInventory#getArmorContents()} 顺序是
 * {@code [靴子, 护腿, 胸甲, 头盔]}，而 mvg 的 {@code armor} 数组按索引决定部位
 * （{@code 0} 头盔、{@code 1} 胸甲、{@code 2} 护腿、{@code 3} 靴子），因此需要反转。
 * */
public final class InventorySlotMapper {

    /** Bukkit 物品栏中快捷栏占用的前 9 个索引 */
    private static final int HOTBAR_SIZE = 9;

    /** mvg 侧快捷栏起始槽位 */
    private static final int MVG_HOTBAR_START = 27;

    /** 装备槽位数量（头盔 / 胸甲 / 护腿 / 靴子） */
    private static final int ARMOR_SIZE = 4;

    private InventorySlotMapper() {}

    /**
     * Bukkit 物品栏索引 → mvg 玩家区槽位编号
     * */
    public static int toMvgSlot(int bukkitIndex) {
        return bukkitIndex < HOTBAR_SIZE ? bukkitIndex + MVG_HOTBAR_START : bukkitIndex - HOTBAR_SIZE;
    }

    /**
     * 玩家背包 + 快捷栏 → mvg {@code slots} 数组（仅包含非空槽位）
     * */
    public static JsonArray storageSlots(PlayerInventory inventory) {
        JsonArray slots = new JsonArray();
        if (inventory == null) {
            return slots;
        }
        ItemStack[] contents = inventory.getStorageContents();
        for (int index = 0; index < contents.length; index++) {
            ItemStack stack = contents[index];
            if (ItemStackCodec.isEmpty(stack)) {
                continue;
            }
            slots.add(ItemStackCodec.toSlot(toMvgSlot(index), stack));
        }
        return slots;
    }

    /**
     * 玩家装备 → mvg {@code armor} 数组
     * <p>
     * 返回固定 4 项：空槽位写成 {@code {"slot": n, "empty": true}}，
     * mvg 会跳过绘制但仍按索引决定部位，因此不能压缩数组。
     * */
    public static JsonArray armorSlots(PlayerInventory inventory) {
        JsonArray armor = new JsonArray();
        ItemStack[] contents = inventory == null ? new ItemStack[ARMOR_SIZE] : inventory.getArmorContents();
        for (int index = 0; index < ARMOR_SIZE; index++) {
            // Bukkit 顺序与 mvg 相反：helmet = contents[3]
            int bukkitIndex = ARMOR_SIZE - 1 - index;
            ItemStack stack = bukkitIndex < contents.length ? contents[bukkitIndex] : null;
            if (ItemStackCodec.isEmpty(stack)) {
                JsonObject empty = new JsonObject();
                empty.addProperty("slot", index);
                empty.addProperty("empty", true);
                armor.add(empty);
            } else {
                armor.add(ItemStackCodec.toSlot(index, stack));
            }
        }
        return armor;
    }

    /**
     * 普通容器（末影箱）→ mvg {@code slots} 数组
     *
     * @param limit 最多读取的槽位数，用于限制绘制行数
     * */
    public static JsonArray containerSlots(Inventory inventory, int limit) {
        JsonArray slots = new JsonArray();
        if (inventory == null) {
            return slots;
        }
        int size = Math.min(inventory.getSize(), limit);
        for (int index = 0; index < size; index++) {
            ItemStack stack = inventory.getItem(index);
            if (ItemStackCodec.isEmpty(stack)) {
                continue;
            }
            slots.add(ItemStackCodec.toSlot(index, stack));
        }
        return slots;
    }

}
