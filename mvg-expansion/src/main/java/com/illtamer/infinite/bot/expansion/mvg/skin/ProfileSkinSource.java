package com.illtamer.infinite.bot.expansion.mvg.skin;

import com.illtamer.infinite.bot.expansion.mvg.util.MainThreadCaller;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * 在线玩家档案来源（正版验证服）
 * <p>
 * 读 {@link PlayerTextures}：正版服上档案里就有贴图地址与模型。
 * 离线服上通常为空 —— 皮肤是 SkinsRestorer 通过协议直接应用给客户端的，
 * 档案里没有 textures，因此这条来源在离线服一般未命中。
 * */
public class ProfileSkinSource implements SkinSource {

    public static final String NAME = SkinData.SOURCE_PROFILE;

    private final Logger logger;

    public ProfileSkinSource(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SkinLookup lookup(UUID uuid, String playerName) {
        try {
            String[] source = MainThreadCaller.call(() -> read(uuid));
            if (source == null) {
                return SkinLookup.miss("档案无纹理");
            }
            return SkinLookup.hit(new TextureRef(source[0], source[1], NAME));
        } catch (RuntimeException e) {
            debug("[Skin] 读取玩家档案皮肤失败: " + uuid, e);
            return SkinLookup.miss("档案读取异常");
        } catch (Error e) {
            // 本体 API 不兼容（链接错误等）不能让整条解析链失效，跳过该来源即可
            if (logger != null) {
                logger.warn("[Skin] 读取玩家档案皮肤时发生链接错误，已跳过该来源: " + uuid, e);
            }
            return SkinLookup.miss("档案 API 不兼容");
        }
    }

    /**
     * 在主线程读取玩家档案中的皮肤地址与模型
     *
     * @return {@code [url, model]}，不可用时返回 {@code null}
     * */
    private String[] read(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return null;
        }
        PlayerTextures textures = player.getPlayerProfile().getTextures();
        if (textures == null || textures.getSkin() == null) {
            return null;
        }
        String model = textures.getSkinModel() == PlayerTextures.SkinModel.SLIM
                ? SkinData.MODEL_SLIM : SkinData.MODEL_CLASSIC;
        return new String[]{textures.getSkin().toString(), model};
    }

    private void debug(String message, Throwable error) {
        if (logger != null) {
            logger.debug(message, error);
        }
    }

}
