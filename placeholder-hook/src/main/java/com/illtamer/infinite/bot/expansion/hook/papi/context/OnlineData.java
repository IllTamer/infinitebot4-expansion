package com.illtamer.infinite.bot.expansion.hook.papi.context;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
public class OnlineData implements Serializable {

    private Set<String> onlineKeys = new HashSet<>();

}
