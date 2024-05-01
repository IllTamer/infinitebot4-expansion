package online.baikele.plugins.botshop;

import java.io.File;

import online.baikele.plugins.botshop.BeasCommands.BeasCommands;
import online.baikele.plugins.botshop.Gui.ShopGuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public class BotShop extends JavaPlugin {
    public void onEnable() {
        this.onload();
        new ExternalExpansionSign().register(this);
        this.getServer().getPluginCommand("botshop").setExecutor(new BeasCommands());
        this.getServer().getPluginManager().registerEvents(new ShopGuiListener(), this);
    }

    public void onDisable() {
        new ExternalExpansionSign().unregister();
    }

    public void onload() {
        this.saveDefaultConfig();
        if (!new File(this.getDataFolder(), "data.yml").exists()) {
            this.saveResource("data.yml", true);
        }

        if (!new File(this.getDataFolder(), "shop.yml").exists()) {
            this.saveResource("shop.yml", true);
        }
    }
}
