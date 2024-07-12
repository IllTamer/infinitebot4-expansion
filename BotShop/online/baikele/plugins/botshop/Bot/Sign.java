package online.baikele.plugins.botshop.Bot;

import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import online.baikele.plugins.botshop.BotShop;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class Sign implements Listener {
    Plugin plugin = BotShop.getPlugin(BotShop.class);

    @EventHandler
    public void onEvent(GroupMessageEvent event) throws IOException {

        String msg = event.getMessage().getCleanMessage().toString();
        //因为api输出有点小问题，所以说临时解决一下前后的括号
        msg = msg.substring(2, msg.length() - 2);

        long userId = event.getSender().getUserId();
        Date date1 = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentTime = sdf.format(date1);
        File dataYML = new File(this.plugin.getDataFolder(), "data.yml");
        YamlConfiguration datas = YamlConfiguration.loadConfiguration(dataYML);
        PlayerData data = StaticAPI.getRepository().queryByUserId(userId);

        if (event.getMessage() != null && msg.equalsIgnoreCase(this.plugin.getConfig().getString("Sign.message")) && StaticAPI.inGroups(event.getGroupId())) {
            if (data == null) {
                event.reply(this.plugin.getConfig().getString("Message.invalid"));
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid()));
            String lasterTime = datas.getString(player.getName() + ".time");
            if (lasterTime != null && lasterTime.equalsIgnoreCase(currentTime)) {
                String msgs = "";

                for (String msgss : this.plugin.getConfig().getStringList("Sign.not")) {
                    msgs = msgs + msgss + "\n";
                }

                event.reply(msgs);
                return;
            }

            String point = this.plugin.getConfig().getString("Sign.points");
            String[] points = point.split("-");
            int d = (int) (Math.random() * (double) (Integer.parseInt(points[1]) - Integer.parseInt(points[0])) + (double) Integer.parseInt(points[0]));
            int pointsnum = datas.getInt(player.getName() + ".points");
            datas.set(player.getName() + ".points", pointsnum + d);
            datas.set(player.getName() + ".time", currentTime);
            datas.save(dataYML);
            int pointsn = datas.getInt(player.getName() + ".points");
            String msgs = "";

            for (String msgss : this.plugin.getConfig().getStringList("Sign.send")) {
                msgs = msgs + msgss.replace("%ap%", "" + d).replace("%p%", "" + pointsn) + "\n";
            }

            event.reply(msgs);
        } else if (event.getMessage() != null
                && msg.equalsIgnoreCase(this.plugin.getConfig().getString("points.message"))
                && StaticAPI.inGroups(event.getGroupId())) {
            if (data == null) {
                event.reply(this.plugin.getConfig().getString("Message.invalid"));
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid()));
            int pointsnum = datas.getInt(player.getName() + ".points");
            String msgs = "";

            for (String msgss : this.plugin.getConfig().getStringList("points.send")) {
                msgs = msgs + msgss.replace("%p%", "" + pointsnum) + "\n";
            }

            event.reply(msgs);
        }
    }
}
