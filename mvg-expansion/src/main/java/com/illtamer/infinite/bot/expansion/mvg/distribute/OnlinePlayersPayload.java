package com.illtamer.infinite.bot.expansion.mvg.distribute;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 子服在线玩家列表的跨服回包
 * */
@Data
@NoArgsConstructor
public class OnlinePlayersPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String serverName;

    private List<OnlinePlayerEntry> players = new ArrayList<>();

    /**
     * 是否因为数量上限被截断
     * */
    private boolean truncated;

    private String error;

    public static OnlinePlayersPayload of(String serverName, List<OnlinePlayerEntry> players) {
        OnlinePlayersPayload payload = new OnlinePlayersPayload();
        payload.setServerName(serverName);
        payload.setPlayers(players);
        return payload;
    }

}
