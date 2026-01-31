package com.illtamer.infinite.bot.expansion.manager.basic.listener;

import com.illtamer.perpetua.sdk.entity.transfer.entity.Client;
import lombok.Getter;

@Getter
public class WaitClientDataException extends RuntimeException {

    private final Client otherClient;

    public WaitClientDataException(Client otherClient) {
        super(otherClient.getAppId());
        this.otherClient = otherClient;
    }

}
