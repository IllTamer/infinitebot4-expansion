package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.illtamer.perpetua.sdk.entity.transfer.entity.Client;
import lombok.Data;

import java.util.List;

@Data
public class DistributeContext<T> {

    /**
     * 触发时当前客户端信息
     * */
    private Client client;

    /**
     * 触发时所有客户端列表
     * @apiNote index=0 处为主节点
     * */
    private List<Client> clientList;

    /**
     * 相应失败的客户端列表
     * */
    private List<Client> failedClientList;

    /**
     * 当前节点+其他节点执行结果汇总
     * */
    private List<T> dataList;

}
