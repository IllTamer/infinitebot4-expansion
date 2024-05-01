package online.baikele.plugins.botshop.Gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import online.baikele.plugins.botshop.BotShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class ShopGui {
    static Plugin plugin = BotShop.getPlugin(BotShop.class);

    public static Inventory Shopgui(Player player) {
        File shopyml = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration shopYml = YamlConfiguration.loadConfiguration(shopyml);
        Inventory inv = Bukkit.createInventory(player, shopYml.getInt("Shop.Size"), shopYml.getString("Shop.Title"));
        int a = 0;

        for (String item : shopYml.getConfigurationSection("Item").getKeys(false)) {
            String newKey = "Item." + item + ".";
            ItemStack itemStack = new ItemStack(Material.valueOf(shopYml.getString(newKey + "item")));
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (shopYml.getString(newKey + "name") != null) {
                itemMeta.setDisplayName(shopYml.getString(newKey + "name"));
            }

            List<String> lore = new ArrayList<>();
            shopYml.getStringList(newKey + "lore").forEach(l -> lore.add(l.replace("&", "§")));

            assert itemMeta != null;

            itemMeta.setLore(lore);
            itemStack.setItemMeta(itemMeta);
            inv.setItem(a, itemStack);
            ++a;
        }

        return inv;
    }
}
