package com.illtamer.infinite.bot.expansion.manager.basic.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class CmdResponse implements Serializable {

    private String response;

    // 服务器名称
    private String clientName;

}
