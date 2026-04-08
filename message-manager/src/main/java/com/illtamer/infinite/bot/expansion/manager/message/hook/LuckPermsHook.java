package com.illtamer.infinite.bot.expansion.manager.message.hook;

import com.illtamer.perpetua.sdk.Pair;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.OfflinePlayer;

import java.time.Instant;

public class LuckPermsHook {

    public static Pair<Boolean, Instant> getPermission(OfflinePlayer player, String permission) {
        User user = getUser(player);
        if (user == null) {
            return new Pair<>(false, null);
        }

        for (Node node : user.getNodes()) {
            if (node instanceof PermissionNode permNode) {
                // 查找存在且 active 的节点
                if (permNode.getPermission().equals(permission) && permNode.getValue()) {
                    return new Pair<>(true, permNode.getExpiry());
                }
            } else if (node instanceof InheritanceNode inheritNode) {
                // lp 返回的权限不带 group. 前缀
                if (inheritNode.getGroupName().equals(permission) && inheritNode.getValue()) {
                    return new Pair<>(true, inheritNode.getExpiry());
                }
            }
        }
        return new Pair<>(false, null);
    }

    private static User getUser(OfflinePlayer player) {
        UserManager manager = LuckPermsProvider.get().getUserManager();
        User user = manager.getUser(player.getUniqueId());
        if (user == null) {
            try {
                user = manager.loadUser(player.getUniqueId()).join();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return user;
    }

}
