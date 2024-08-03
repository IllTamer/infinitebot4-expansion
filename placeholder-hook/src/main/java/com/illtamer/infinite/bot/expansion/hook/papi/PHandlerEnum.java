package com.illtamer.infinite.bot.expansion.hook.papi;

import com.illtamer.infinite.bot.minecraft.configuration.config.BotConfiguration;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.perpetua.sdk.entity.transfer.entity.GroupMember;
import com.illtamer.perpetua.sdk.handler.OpenAPIHandling;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PHandlerEnum {

    /**
     * @apiNote 如果有注册对应变量的需求，获取本 list 后添加对应实现即可
     * */
    public static final List<PHandlerEnum> HANDLER_LIST = new LinkedList<>();

    private final String subId;
    private final PHandler pHandler;

    public PHandlerEnum(String subId, PHandler pHandler) {
        this.subId = subId;
        this.pHandler = pHandler;
    }

    static {
        HANDLER_LIST.addAll(Arrays.asList(
                // 获取玩家绑定的 qq
                new PHandlerEnum("get-bind-qq", (offlinePlayer, arg) -> {
                    if (offlinePlayer == null) {
                        return PHandler.ONLY_PLAYER;
                    }
                    PlayerData data = BotConfiguration.getInstance().getRepository().queryByUUID(offlinePlayer.getUniqueId());
                    if (data == null) {
                        return PHandler.UNBIND;
                    }
                    return String.valueOf(data.getUserId());
                }),

                // 检查玩家是否在机器人也存在的某个群组中
                new PHandlerEnum("check-in-group", (offlinePlayer, arg) -> {
                    if (offlinePlayer == null) {
                        return PHandler.ONLY_PLAYER;
                    }
                    Long group;
                    if (arg == null || (group = StringHelper.parseLong(arg)) == null) {
                        return PHandler.WRONG_ARG;
                    }
                    PlayerData data = BotConfiguration.getInstance().getRepository().queryByUUID(offlinePlayer.getUniqueId());
                    if (data == null) {
                        return PHandler.UNBIND;
                    }
                    boolean present = OpenAPIHandling.getGroups().stream()
                            .anyMatch(g -> g.getGroupId().longValue() == group.longValue());
                    return Boolean.toString(present);
                }),

                // 获取玩家在指定群组中的群名片
                new PHandlerEnum("get-group-card", (offlinePlayer, arg) -> {
                    if (offlinePlayer == null) {
                        return PHandler.ONLY_PLAYER;
                    }
                    Long group;
                    if (arg == null || (group = StringHelper.parseLong(arg)) == null) {
                        return PHandler.WRONG_ARG;
                    }
                    PlayerData data = BotConfiguration.getInstance().getRepository().queryByUUID(offlinePlayer.getUniqueId());
                    if (data == null) {
                        return PHandler.UNBIND;
                    }
                    GroupMember member = OpenAPIHandling.getGroupMember(group, data.getUserId(), false);
                    if (member == null) {
                        return PHandler.NO_DATA;
                    }
                    return member.getCard() == null ? member.getNickname() : member.getCard();
                })
        ));
    }

    public String getSubId() {
        return subId;
    }

    public PHandler getPHandler() {
        return pHandler;
    }

}
