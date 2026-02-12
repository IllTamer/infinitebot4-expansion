package com.illtamer.infinite.bot.expansion.manager.basic.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class DataOnShowPlayers implements Serializable {

    // op 名称列表
    private List<String> opList = new ArrayList<>();

    // 玩家名称列表
    private List<String> playerList = new ArrayList<>();

    // 服务器名称
    private String clientName;

    public boolean isEmpty() {
        return opList.isEmpty() && playerList.isEmpty();
    }

}
