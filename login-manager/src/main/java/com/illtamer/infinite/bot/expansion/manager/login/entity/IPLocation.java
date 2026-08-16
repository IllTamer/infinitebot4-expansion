package com.illtamer.infinite.bot.expansion.manager.login.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * IP 归属地
 * */
@Data
@AllArgsConstructor
public class IPLocation {

    private String province;
    private String city;
    private String isp;

    public static IPLocation of(String fallback) {
        return new IPLocation(fallback, fallback, fallback);
    }

}
