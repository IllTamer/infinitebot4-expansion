package com.illtamer.infinite.bot.expansion.mvg.skin;

import java.util.UUID;

/**
 * SkinsRestorer 来源
 * <p>
 * 只读本地存储（外加 SkinsRestorer 自己的按名解析），不下载贴图；
 * 未安装 SkinsRestorer 或 API 未就绪时返回未命中原因。
 * */
public class SkinsRestorerSource implements SkinSource {

    public static final String NAME = SkinData.SOURCE_SKINSRESTORER;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SkinLookup lookup(UUID uuid, String playerName) {
        if (!SkinsRestorerHook.isAvailable()) {
            return SkinLookup.miss("未接入");
        }
        TextureValueParser.Result texture = SkinsRestorerHook.resolve(uuid, playerName);
        if (texture == null) {
            return SkinLookup.miss("该玩家没有绑定皮肤");
        }
        return SkinLookup.hit(new TextureRef(texture.getUrl(), texture.getModel(), NAME));
    }

}
