package online.baikele.plugins.botshop;

import com.illtamer.infinite.bot.minecraft.api.EventExecutor;
import com.illtamer.infinite.bot.minecraft.api.IExpansion;
import com.illtamer.infinite.bot.minecraft.expansion.ExpansionConfig;
import com.illtamer.infinite.bot.minecraft.expansion.manager.AbstractExternalExpansion;
import online.baikele.plugins.botshop.Bot.Sign;

public class ExternalExpansionSign extends AbstractExternalExpansion {
    private IExpansion instance;
    private ExpansionConfig config;
    private ExpansionConfig data;
    private ExpansionConfig shop;

    public void onEnable() {
        this.instance = this;
        EventExecutor.registerEvents(new Sign(), this.instance);
    }

    public void onDisable() {
        this.instance = null;
    }

    public String getExpansionName() {
        return "ExternalExpansionSign";
    }

    public String getVersion() {
        return "1.2-SNAPSHOT";
    }

    public String getAuthor() {
        return "WhiteCola And NeglectDream";
    }
}
