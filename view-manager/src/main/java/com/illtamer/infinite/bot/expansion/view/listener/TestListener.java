package com.illtamer.infinite.bot.expansion.view.listener;

import com.illtamer.infinite.bot.api.event.message.GroupMessageEvent;
import com.illtamer.infinite.bot.expansion.view.util.DispatchUtil;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.api.event.Priority;
import com.loohp.interactivechat.api.InteractiveChatAPI;
import com.loohp.interactivechat.objectholders.OfflineICPlayer;
import com.loohp.interactivechat.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class TestListener implements Listener {

    @EventHandler(priority = Priority.HIGHEST)
    public void onTest(GroupMessageEvent event) {
        System.out.println(event.getMessage());
        if (!StaticAPI.inGroups(event.getGroupId())) return;
        final String rawMessage = event.getRawMessage();
        if (!rawMessage.startsWith("测试 ")) return;

        final UUID uuid = DispatchUtil.executeImageWrapper(rawMessage.substring("测试 ".length()), true);
        event.reply("图片已提交: " + uuid.toString());
//        OfflineICPlayer offlineICPlayer = ICPlayerFactory.getOfflineICPlayer(uuid);
    }

    private static void ender(OfflineICPlayer player, String sha1, String title) throws Exception {
        int size = player.getEnderChest().getSize();
        Inventory inv = Bukkit.createInventory(null, InventoryUtils.toMultipleOf9(size), title);
        for (int j = 0; j < size; j++) {
            if (player.getEnderChest().getItem(j) != null) {
                if (!player.getEnderChest().getItem(j).getType().equals(Material.AIR)) {
                    inv.setItem(j, player.getEnderChest().getItem(j).clone());
                }
            }
        }

        InteractiveChatAPI.addInventoryToItemShareList(InteractiveChatAPI.SharedType.ENDERCHEST, sha1, inv);
    }

}
