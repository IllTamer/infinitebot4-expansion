package com.illtamer.infinite.bot.expansion.test;

import com.illtamer.infinite.bot.expansion.test.hook.IB3Expansion;
import org.bukkit.plugin.java.JavaPlugin;

public class TestMain extends JavaPlugin {

    @Override
    public void onEnable() {
        new IB3Expansion().register(this);
    }

}
