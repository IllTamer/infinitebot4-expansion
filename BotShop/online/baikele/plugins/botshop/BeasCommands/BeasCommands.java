package online.baikele.plugins.botshop.BeasCommands;

import java.io.File;
import java.io.IOException;

import online.baikele.plugins.botshop.BotShop;
import online.baikele.plugins.botshop.Gui.ShopGui;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BeasCommands implements CommandExecutor {
    Plugin plugin = BotShop.getPlugin(BotShop.class);

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage("§a插件指令");
            sender.sendMessage("§c♦ §a/botshop gui §f➥ 打开积分商店");
            if (player.hasPermission("botshop.admin")) {
                sender.sendMessage("§c♦ §a/botshop add [player] [num] §f➥ 给玩家添加积分");
                sender.sendMessage("§c♦ §a/botshop take [player] [num] §f➥ 扣除玩家的积分");
                sender.sendMessage("§c♦ §a/botshop reload §f➥ 重载配置文件");
            }

            return true;
        } else {
            String notpremission = this.plugin.getConfig().getString("Message.notpremission").replace("&", "§");
            String var7 = args[0];
            switch (var7) {
                case "gui":
                    player.openInventory(ShopGui.Shopgui(player));
                    break;
                case "reload":
                    if (player.hasPermission("botshop.admin")) {
                        this.onreload(player);
                    } else {
                        player.sendMessage(notpremission);
                    }
                    break;
                case "take":
                    if (args.length == 1 && player.hasPermission("botshop.admin")) {
                        player.sendMessage("请输入玩家ID");
                        return true;
                    }

                    if (args.length == 2 && player.hasPermission("botshop.admin")) {
                        player.sendMessage("请输入一个数值");
                        return true;
                    }

                    if (player.hasPermission("botshop.admin")) {
                        try {
                            this.ontoke(args[1], Integer.parseInt(args[2]));
                        } catch (IOException ignored) {
                        }
                    } else {
                        player.sendMessage(notpremission);
                    }
                    break;
                case "add":
                    if (args.length == 1 && player.hasPermission("botshop.admin")) {
                        player.sendMessage("请输入玩家ID");
                        return true;
                    }

                    if (args.length == 2 && player.hasPermission("botshop.admin")) {
                        player.sendMessage("请输入一个数值");
                        return true;
                    }

                    if (player.hasPermission("botshop.admin")) {
                        try {
                            this.onadd(args[1], Integer.parseInt(args[2]));
                        } catch (IOException ignored) {
                        }
                    } else {
                        player.sendMessage(notpremission);
                    }
            }

            return true;
        }
    }

    private void onreload(Player player) {
        this.plugin.reloadConfig();
        File shop = new File(this.plugin.getDataFolder(), "shop.yml");
        File data = new File(this.plugin.getDataFolder(), "data.yml");
        FileConfiguration datas = YamlConfiguration.loadConfiguration(data);
        FileConfiguration shops = YamlConfiguration.loadConfiguration(shop);
        String reloadmsg = this.plugin.getConfig().getString("Message.reload").replace("&", "§");
        player.sendMessage(reloadmsg);
    }

    private void ontoke(String name, int num) throws IOException {
        Player player = Bukkit.getPlayer(name);
        File data = new File(this.plugin.getDataFolder(), "data.yml");
        YamlConfiguration datayml = YamlConfiguration.loadConfiguration(data);
        int points = datayml.getInt(player.getName() + ".points");
        datayml.set(player.getName() + ".points", points - num);
        String tokemsg = this.plugin.getConfig().getString("Message.tokemsg").replace("&", "§").replace("%p%", String.valueOf(num));
        player.sendMessage(tokemsg);
        datayml.save(data);
    }

    private void onadd(String name, int num) throws IOException {
        Player player = Bukkit.getPlayer(name);
        File data = new File(this.plugin.getDataFolder(), "data.yml");
        YamlConfiguration datayml = YamlConfiguration.loadConfiguration(data);
        int points = datayml.getInt(player.getName() + ".points");
        datayml.set(player.getName() + ".points", points + num);
        String addmsg = this.plugin.getConfig().getString("Message.addmsg").replace("&", "§").replace("%p%", String.valueOf(num));
        player.sendMessage(addmsg);
        datayml.save(data);
    }
}
