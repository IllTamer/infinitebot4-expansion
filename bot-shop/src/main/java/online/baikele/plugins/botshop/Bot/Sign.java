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

    private final Plugin plugin = BotShop.getPlugin(BotShop.class);
    private final File dataYML;
    private final YamlConfiguration datas;

    public Sign() {
        this.dataYML = new File(this.plugin.getDataFolder(), "data.yml");
        this.datas = YamlConfiguration.loadConfiguration(dataYML);
    }

    @EventHandler
    public void onEvent(GroupMessageEvent event) throws IOException {
        String msg = event.getRawMessage();

        long userId = event.getSender().getUserId();
        Date date1 = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentTime = sdf.format(date1);
        PlayerData data = StaticAPI.getRepository().queryByUserId(userId);

        if (!msg.isEmpty() && StaticAPI.inGroups(event.getGroupId()) && msg.equalsIgnoreCase(this.plugin.getConfig().getString("Sign.message"))) {
            if (data == null) {
                event.reply(this.plugin.getConfig().getString("Message.invalid"));
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid()));
            String lasterTime = datas.getString(player.getName() + ".time");
            if (lasterTime != null && lasterTime.equalsIgnoreCase(currentTime)) {
                StringBuilder msgs = new StringBuilder();

                for (String msgss : this.plugin.getConfig().getStringList("Sign.not")) {
                    msgs.append(msgss).append("\n");
                }

                event.reply(msgs.toString());
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
            StringBuilder msgs = new StringBuilder();

            for (String msgss : this.plugin.getConfig().getStringList("Sign.send")) {
                msgs.append(msgss.replace("%ap%", "" + d).replace("%p%", "" + pointsn)).append("\n");
            }

            event.reply(msgs.toString());
        } else if (!msg.isEmpty()
                && StaticAPI.inGroups(event.getGroupId())
                && msg.equalsIgnoreCase(this.plugin.getConfig().getString("points.message"))
        ) {
            if (data == null) {
                event.reply(this.plugin.getConfig().getString("Message.invalid"));
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid()));
            int pointsnum = datas.getInt(player.getName() + ".points");
            StringBuilder msgs = new StringBuilder();

            for (String msgss : this.plugin.getConfig().getStringList("points.send")) {
                msgs.append(msgss.replace("%p%", "" + pointsnum)).append("\n");
            }

            event.reply(msgs.toString());
        }
    }
}
