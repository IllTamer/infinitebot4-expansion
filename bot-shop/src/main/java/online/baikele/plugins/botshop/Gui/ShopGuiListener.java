package online.baikele.plugins.botshop.Gui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import me.clip.placeholderapi.PlaceholderAPI;
import online.baikele.plugins.botshop.BotShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;

public class ShopGuiListener implements Listener {
    Plugin plugin = BotShop.getPlugin(BotShop.class);

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        File shop = new File(this.plugin.getDataFolder(), "shop.yml");
        YamlConfiguration shopyml = YamlConfiguration.loadConfiguration(shop);
        File data = new File(this.plugin.getDataFolder(), "data.yml");
        YamlConfiguration datayml = YamlConfiguration.loadConfiguration(data);
        Player player = (Player) event.getWhoClicked();

        try {
            InventoryView inventoryView = event.getView();
            if (inventoryView.getTitle().equals(shopyml.get("Shop.Title"))) {
                if (event.getCurrentItem().getType() == Material.AIR) {
                    return;
                }

                int point = datayml.getInt(player.getName() + ".points");
                int count = 0;

                for (String item : shopyml.getConfigurationSection("Item").getKeys(false)) {
                    if (count == event.getRawSlot()) {
                        int points = shopyml.getInt("Item." + item + ".points");
                        List<String> cmd = shopyml.getStringList("Item." + item + ".command");
                        if (point >= points) {
                            command(player, cmd);
                            datayml.set(player.getName() + ".points", point - points);
                        } else {
                            player.sendMessage(this.plugin.getConfig().getString("Message.notpoints"));
                        }

                        inventoryView.close();

                        try {
                            datayml.save(data);
                        } catch (IOException ignored) {
                        }
                        break;
                    }

                    ++count;
                }

                event.setCancelled(true);
            }
        } catch (NullPointerException ignored) {
        }
    }

    public static void command(Player player, List cmdss) {
        for (Object key : cmdss) {
            String cmd = (String) key;
            String[] cmds = cmd.split(":");
            cmds[1] = cmds[1].replace("&", "§").replace("%player%", player.getName());
            String string = cmds[1];
            String var7 = cmds[0];
            switch (var7) {
                case "console":
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(player, string));
                    break;
                case "op":
                    boolean wasOp = player.isOp();
                    try {
                        player.setOp(true);
                        player.chat("/" + PlaceholderAPI.setPlaceholders(player, string)); 
                    } finally {
                        player.setOp(wasOp);
                    }
                    break;
                case "tell":
                    player.sendMessage(PlaceholderAPI.setPlaceholders(player, string));
                    break;
                case "bc":
                    Bukkit.broadcastMessage(PlaceholderAPI.setPlaceholders(player, string));
                    break;
                case "player":
                    player.chat("/" + PlaceholderAPI.setPlaceholders(player, string));
                    break;
                case "chat":
                    player.chat(PlaceholderAPI.setPlaceholders(player, string));
            }
        }
    }
}
