package com.illtamer.infinite.bot.expansion.mvg.skin;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家皮肤材质缓存
 * <p>
 * 内存缓存 + 磁盘缓存。磁盘写入的是原始 PNG 与模型标识，读取时重新编码为 data URI，
 * 避免把 base64 文本直接落盘（体积大且不便排查）。
 * */
public class SkinCache {

    private static final String SUFFIX_PNG = ".png";
    private static final String SUFFIX_MODEL = ".model";
    private static final String SUFFIX_SOURCE = ".src";

    private final File skinFolder;
    private final long ttlMillis;
    private final Logger logger;
    private final Map<UUID, SkinData> memory = new ConcurrentHashMap<>(64);

    private final SkinData defaultClassic;
    private final SkinData defaultSlim;

    public SkinCache(File dataFolder, long ttlMillis, InputStream classicPng, InputStream slimPng, Logger logger) {
        this.skinFolder = new File(dataFolder, "skins");
        this.ttlMillis = Math.max(0L, ttlMillis);
        this.logger = logger;
        if (!skinFolder.exists() && !skinFolder.mkdirs()) {
            logger.warn("[Skin] 无法创建皮肤缓存目录: " + skinFolder.getAbsolutePath());
        }
        this.defaultClassic = readDefault(classicPng, SkinData.MODEL_CLASSIC);
        this.defaultSlim = readDefault(slimPng, SkinData.MODEL_SLIM);
    }

    private SkinData readDefault(InputStream input, String model) {
        if (input == null) {
            return new SkinData("", model, System.currentTimeMillis(), SkinData.SOURCE_DEFAULT);
        }
        try (InputStream stream = input) {
            byte[] bytes = stream.readAllBytes();
            if (bytes.length == 0) {
                return new SkinData("", model, System.currentTimeMillis(), SkinData.SOURCE_DEFAULT);
            }
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            return new SkinData(dataUri, model, System.currentTimeMillis(), SkinData.SOURCE_DEFAULT);
        } catch (IOException e) {
            logger.warn("[Skin] 读取内置默认皮肤失败(" + model + ")", e);
            return new SkinData("", model, System.currentTimeMillis(), SkinData.SOURCE_DEFAULT);
        }
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    /**
     * 默认皮肤（模型不可用时按 classic 兜底）
     * */
    public SkinData getDefault(String model) {
        if (SkinData.MODEL_SLIM.equalsIgnoreCase(model) && !defaultSlim.getDataUri().isEmpty()) {
            return defaultSlim;
        }
        if (!defaultClassic.getDataUri().isEmpty()) {
            return defaultClassic;
        }
        return SkinData.MODEL_SLIM.equalsIgnoreCase(model) ? defaultSlim : defaultClassic;
    }

    /**
     * 只读内存缓存（含过期校验），不触碰磁盘
     * <p>
     * 供主线程路径使用：分布式回包组装、在线列表采集等场景不能承受文件 IO
     * */
    public SkinData getInMemory(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        SkinData cached = memory.get(uuid);
        if (cached == null || cached.isExpired(ttlMillis)) {
            return null;
        }
        return cached;
    }

    /**
     * 读取有效缓存（内存 → 磁盘），未命中或已过期返回 {@code null}
     * <p>
     * 含文件读取与 Base64 编码，只能在异步线程调用
     * */
    public SkinData get(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        SkinData cached = memory.get(uuid);
        if (cached != null) {
            if (!cached.isExpired(ttlMillis)) {
                return cached;
            }
            memory.remove(uuid);
        }
        SkinData fromDisk = loadFromDisk(uuid);
        if (fromDisk != null) {
            memory.put(uuid, fromDisk);
        }
        return fromDisk;
    }

    /**
     * 无条件写入缓存（内存 + 磁盘）
     * */
    public void put(UUID uuid, SkinData data) {
        if (uuid == null || data == null || data.getDataUri().isEmpty()) {
            return;
        }
        memory.put(uuid, data);
        writeToDisk(uuid, data);
    }

    public void remove(UUID uuid) {
        if (uuid == null) {
            return;
        }
        memory.remove(uuid);
        deleteQuietly(new File(skinFolder, uuid + SUFFIX_PNG));
        deleteQuietly(new File(skinFolder, uuid + SUFFIX_MODEL));
        deleteQuietly(new File(skinFolder, uuid + SUFFIX_SOURCE));
    }

    public void clear() {
        memory.clear();
    }

    public int size() {
        return memory.size();
    }

    private SkinData loadFromDisk(UUID uuid) {
        File png = new File(skinFolder, uuid + SUFFIX_PNG);
        if (!png.isFile()) {
            return null;
        }
        long lastModified = png.lastModified();
        if (ttlMillis > 0 && System.currentTimeMillis() - lastModified > ttlMillis) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(png.toPath());
            if (bytes.length == 0) {
                return null;
            }
            String model = readModel(uuid);
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            return new SkinData(dataUri, model, lastModified, SkinData.SOURCE_DISK, readSourceKey(uuid));
        } catch (IOException e) {
            logger.debug("[Skin] 读取磁盘皮肤失败: " + uuid, e);
            return null;
        }
    }

    /**
     * 读取来源指纹；历史缓存没有该文件时返回空串（表示无从校验，继续沿用缓存）
     * */
    private String readSourceKey(UUID uuid) {
        File file = new File(skinFolder, uuid + SUFFIX_SOURCE);
        if (!file.isFile()) {
            return "";
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private String readModel(UUID uuid) {
        File model = new File(skinFolder, uuid + SUFFIX_MODEL);
        if (!model.isFile()) {
            return SkinData.MODEL_CLASSIC;
        }
        try {
            String value = Files.readString(model.toPath(), StandardCharsets.UTF_8).trim();
            return SkinData.MODEL_SLIM.equalsIgnoreCase(value) ? SkinData.MODEL_SLIM : SkinData.MODEL_CLASSIC;
        } catch (IOException e) {
            return SkinData.MODEL_CLASSIC;
        }
    }

    private void writeToDisk(UUID uuid, SkinData data) {
        if (data.isDefault()) {
            // 默认皮肤不入盘，避免把兜底数据当成玩家真实皮肤长期缓存
            return;
        }
        String base64 = data.getDataUri();
        int index = base64.indexOf("base64,");
        if (index < 0) {
            return;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64.substring(index + "base64,".length()));
            Path png = new File(skinFolder, uuid + SUFFIX_PNG).toPath();
            Files.write(png, bytes);
            Files.write(new File(skinFolder, uuid + SUFFIX_MODEL).toPath(),
                    data.getModel().getBytes(StandardCharsets.UTF_8));
            // 指纹与 PNG 一起落盘：下次命中缓存时据此判断玩家是否换过皮肤
            if (!data.getSourceKey().isEmpty()) {
                Files.write(new File(skinFolder, uuid + SUFFIX_SOURCE).toPath(),
                        data.getSourceKey().getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException | IllegalArgumentException e) {
            logger.debug("[Skin] 写入磁盘皮肤失败: " + uuid, e);
        }
    }

    private void deleteQuietly(File file) {
        try {
            if (file.isFile() && !file.delete()) {
                logger.debug("[Skin] 无法删除缓存文件: " + file.getAbsolutePath());
            }
        } catch (RuntimeException e) {
            logger.debug("[Skin] 删除缓存文件异常: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 校验 PNG 是否为 mvg 接受的皮肤尺寸（64×64 或 legacy 64×32）
     * */
    public static boolean isValidSkinSize(int width, int height) {
        return (width == 64 && height == 64) || (width == 64 && height == 32);
    }

}
