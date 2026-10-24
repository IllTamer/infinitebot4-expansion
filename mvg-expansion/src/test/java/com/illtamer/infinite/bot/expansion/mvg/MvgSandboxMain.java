package com.illtamer.infinite.bot.expansion.mvg;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgApiException;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgClient;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgRenderResult;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgResourceState;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgResourceWarmer;
import com.illtamer.infinite.bot.expansion.mvg.config.MvgConfig;
import com.illtamer.infinite.bot.expansion.mvg.distribute.OnlinePlayerEntry;
import com.illtamer.infinite.bot.expansion.mvg.distribute.PlayerSnapshotPayload;
import com.illtamer.infinite.bot.expansion.mvg.lang.ItemNameTable;
import com.illtamer.infinite.bot.expansion.mvg.render.CustomDataSupport;
import com.illtamer.infinite.bot.expansion.mvg.render.ItemStackCodec;
import com.illtamer.infinite.bot.expansion.mvg.render.MvgRenderService;
import com.illtamer.infinite.bot.expansion.mvg.render.TooltipBuilder;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinCache;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinData;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinLookup;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinResolver;
import com.illtamer.infinite.bot.expansion.mvg.skin.SkinSource;
import com.illtamer.infinite.bot.expansion.mvg.skin.TextureRef;
import com.illtamer.infinite.bot.expansion.mvg.util.LegacyText;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 离线沙箱 / 端到端验收
 * <p>
 * 直接对本地 mvg 渲染服务生成并发送测试报文，逐项断言图片尺寸与内容差异：
 * 放大倍率、皮肤是否真的传给了 mvg、tooltip 正文（{@code item.lore}）、
 * ItemsAdder 自定义物品（{@code customData}）。
 * <p>
 * 每次请求的 JSON 都会写入 {@code build/sandbox/*.json}，可以直接用 curl 复跑：
 * <pre>
 * curl -X POST http://127.0.0.1:4666/api/v1/render \
 *      -H 'Content-Type: application/json' \
 *      --data-binary @build/sandbox/player-inventory-default.json -o out.png
 * </pre>
 * <p>
 * 运行方式：
 * <pre>
 * ./gradlew :mvg-expansion:sandbox
 * ./gradlew :mvg-expansion:sandbox -Pskin=/path/to/64x64.png
 * ./gradlew :mvg-expansion:sandbox -PiaMaterial=paper -PiaCustomData=11257
 * </pre>
 * 任一断言失败时进程以非 0 退出。
 * */
public final class MvgSandboxMain {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:4666";

    /** 沙箱请求的放大倍率，与 config.yml 的默认值一致 */
    private static final double SCALE = 2D;

    private static final Path OUTPUT = Paths.get("build", "sandbox");

    private static final List<String> PASSED = new ArrayList<>();
    private static final List<String> FAILED = new ArrayList<>();

    private MvgSandboxMain() {}

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : DEFAULT_BASE_URL;
        Files.createDirectories(OUTPUT);
        Logger logger = LoggerFactory.getLogger("sandbox");

        MvgConfig config = sandboxConfig(baseUrl);
        RecordingClient client = new RecordingClient(config, logger);
        SkinCache skinCache = createSkinCache(logger);
        SkinResolver skinResolver = new SkinResolver(skinCache, 256 * 1024, 5, logger);
        MvgResourceWarmer warmer = new MvgResourceWarmer(client, MvgClient.SUPPORTED_VERSION,
                3, 30, 0, logger);
        // 渲染验收不依赖预热状态机，直接跳过；状态机本身在最后单独验证
        warmer.bypass();
        MvgRenderService renderService = new MvgRenderService(client, warmer, config, skinResolver, logger);

        System.out.println("=== mvg 存活检查（GET /api/v1/versions）===");
        check("服务返回版本列表", client.isHealthy());
        ensureResources(client);

        System.out.println("=== 0/10 皮肤缓存：内存清空后仍能从磁盘回落 ===");
        checkSkinCache(skinCache, skinResolver);

        String defaultSkin = skinResolver.getDefault(null).getDataUri();
        check("内置默认皮肤可用", defaultSkin.startsWith("data:image/png;base64,"));

        System.out.println("=== 1/10 玩家背包（默认皮肤，scale=" + SCALE + "）===");
        MvgRenderResult inventoryDefault = renderService.renderInventory(inventoryPayload(defaultSkin));
        save("player-inventory-default", client.lastPayload(), inventoryDefault);
        expectSize("背包尺寸 = 176x166 x2", inventoryDefault, 352, 332);

        System.out.println("=== 2/10 玩家背包（真实皮肤，验证 base64 是否真的传到了 mvg）===");
        verifyRealSkin(renderService, client, skinResolver, inventoryDefault);

        System.out.println("=== 3/10 末影箱（scale=" + SCALE + "）===");
        MvgRenderResult enderChest = renderService.renderEnderChest(enderChestPayload());
        save("enderchest", client.lastPayload(), enderChest);
        expectSize("末影箱尺寸 = 176x167 x2", enderChest, 352, 334);

        System.out.println("=== 4/10 tooltip：正文走 item.lore，不能被丢弃 ===");
        verifyTooltip(client);

        System.out.println("=== 5/10 服务器在线（3 人，scale=" + SCALE + "）===");
        MvgRenderResult playerList = renderService.renderPlayerList(onlinePlayers(defaultSkin));
        save("playerlist", client.lastPayload(), playerList);
        expectSize("在线列表尺寸 = 360x98 x2", playerList, 720, 196);

        System.out.println("=== 6/10 ItemsAdder 自定义物品（customData）===");
        verifyCustomData(client);

        System.out.println("=== 7/10 预热状态机与错误路径 ===");
        MvgResourceWarmer realWarmer = new MvgResourceWarmer(client, MvgClient.SUPPORTED_VERSION,
                3, 30, 0, logger);
        check("预热状态机初始不可用", !realWarmer.isReady());
        realWarmer.bypass();
        check("跳过后立即可用", realWarmer.isReady());
        expectRejected("缺少必填字段的 playerInventory 被拒绝", () ->
                client.render(client.buildRenderPayload("playerInventory", new JsonObject())));

        System.out.println("=== 8/10 customData 字段位置与下发范围 ===");
        verifyCustomDataFields();

        System.out.println("=== 9/10 tooltip 行序与属性合并显示 ===");
        verifyTooltipLayout();

        System.out.println("=== 10/10 皮肤来源指纹：换肤重新下载、来源失效保留缓存 ===");
        verifySkinFingerprint(logger);

        System.out.println();
        System.out.println("通过 " + PASSED.size() + " 项，失败 " + FAILED.size() + " 项");
        for (String failure : FAILED) {
            System.out.println("  FAIL " + failure);
        }
        System.out.println("产物与测试报文目录: " + OUTPUT.toAbsolutePath());
        if (!FAILED.isEmpty()) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ 各项验收

    /**
     * 皮肤缓存回落：写入磁盘 → 清空内存 → 仍能拿到真实皮肤
     * */
    private static void checkSkinCache(SkinCache cache, SkinResolver resolver) throws Exception {
        String dataUri = defaultDataUri();
        UUID uuid = UUID.randomUUID();
        cache.put(uuid, new SkinData(dataUri, SkinData.MODEL_CLASSIC,
                System.currentTimeMillis(), SkinData.SOURCE_DISK));
        cache.clear();
        check("内存清空后 warmCache 仍命中磁盘", resolver.warmCache(uuid));
        cache.clear();
        SkinData skin = resolver.getCachedOrDefault(uuid, null);
        check("内存清空后不再退回默认皮肤", !skin.isDefault());
        check("皮肤仍是内联 PNG data URI", skin.getDataUri().startsWith("data:image/png;base64,"));
        check("皮肤内容与写入的一致", skin.getDataUri().equals(dataUri));
    }

    private static void verifyRealSkin(MvgRenderService renderService, RecordingClient client,
                                       SkinResolver resolver, MvgRenderResult baseline) throws Exception {
        String path = System.getProperty("mvg.sandbox.skin");
        if (path == null || path.trim().isEmpty()) {
            System.out.println("  SKIP 未提供 -Pskin=/path/to/64x64.png，跳过真实皮肤对比");
            return;
        }
        String dataUri = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(path.trim())));
        MvgRenderResult real = renderService.renderInventory(inventoryPayload(dataUri));
        save("player-inventory-real-skin", client.lastPayload(), real);
        expectSize("真实皮肤背包尺寸一致", real, baseline.getWidth(), baseline.getHeight());
        check("真实皮肤与默认皮肤的渲染结果不同（皮肤确实被使用）", !sameImage(real, baseline));
        check("payload 中携带真实皮肤", client.lastPayload().toString().contains(dataUri));
        check("皮肤来源非默认", !resolver.getDefault(null).getDataUri().equals(dataUri));
    }

    /**
     * tooltip：正文来自 {@code item.lore}，不能被服务端忽略
     * <p>
     * 空内容高度 = 行数 × 10 + 24（单行再减 2），缩放后整体乘以 {@link #SCALE}。
     * */
    private static void verifyTooltip(RecordingClient client) throws Exception {
        JsonObject control = tooltipData(new JsonArray());
        MvgRenderResult controlResult = client.render(client.buildRenderPayload("tooltip", control, SCALE));
        save("tooltip-title-only", client.lastPayload(), controlResult);
        expectSize("只有标题的 tooltip 高度 = (10-2+24) x2", controlResult, controlResult.getWidth(), 64);

        JsonArray lines = new JsonArray();
        lines.add("\u00A75\u00A7o深海锻造");
        lines.add("\u00A77锋利 \u00A77V");
        lines.add("\u00A77在主手时：");
        lines.add("\u00A79+7 攻击伤害");
        lines.add("\u00A77耐久度: 1549 / 1561");
        JsonObject full = tooltipData(lines);
        MvgRenderResult fullResult = client.render(client.buildRenderPayload("tooltip", full, SCALE));
        save("tooltip-with-lore", client.lastPayload(), fullResult);
        expectSize("标题 + 5 行正文高度 = (6x10+24) x2", fullResult, fullResult.getWidth(), 168);
        check("正文没有被丢弃（比只有标题更高）", fullResult.getHeight() > controlResult.getHeight());
        check("payload 使用 item.lore 而不是已移除的 lines",
                client.lastPayload().toString().contains("\"lore\"")
                        && !client.lastPayload().toString().contains("\"lines\""));
    }

    private static void verifyCustomData(RecordingClient client) throws Exception {
        String material = System.getProperty("mvg.sandbox.iaMaterial", "paper");
        String rawCustomData = System.getProperty("mvg.sandbox.iaCustomData", "11257");
        int customData;
        try {
            customData = Integer.parseInt(rawCustomData.trim());
        } catch (NumberFormatException e) {
            System.out.println("  SKIP -PiaCustomData 不是整数: " + rawCustomData);
            return;
        }
        // 重建索引让 resolved 归零，这样"这次是否命中索引"才能被观察到；
        // 只丢弃自定义物品的模型/贴图缓存，随后渲染会按需重建
        try {
            client.reloadCustom();
        } catch (MvgApiException e) {
            System.out.println("  WARN 无法重建自定义物品索引: " + e.getMessage());
        }

        JsonObject vanilla = new JsonObject();
        vanilla.addProperty("id", "minecraft:" + material);
        vanilla.addProperty("count", 1);
        MvgRenderResult vanillaResult = client.render(client.buildRenderPayload("item", vanilla, SCALE));
        save("item-vanilla", client.lastPayload(), vanillaResult);

        JsonObject custom = vanilla.deepCopy();
        custom.addProperty("customData", customData);
        MvgRenderResult customResult = client.render(client.buildRenderPayload("item", custom, SCALE));
        save("item-itemsadder", client.lastPayload(), customResult);
        int resolved = resolvedCustomItems(client);

        check("payload 携带 customData", client.lastPayload().toString().contains("\"customData\""));
        expectSize("自定义物品尺寸与原版一致", customResult,
                vanillaResult.getWidth(), vanillaResult.getHeight());
        if (!sameImage(customResult, vanillaResult)) {
            check("mvg 命中 ItemsAdder 索引，渲染结果与原版不同", true);
            check("索引解析计数 > 0（resolved=" + resolved + "）", resolved > 0);
        } else {
            System.out.println(String.format(
                    "  SKIP mvg 索引里没有 %s#%d（该机器未配置对应物品包，resolved=%d），只校验报文结构",
                    material, customData, resolved));
        }
    }

    /**
     * customData 的字段位置与下发范围
     * <p>
     * mvg 只认物品级 {@code item.customData}：{@code item.customModelData}、
     * {@code components["minecraft:custom_model_data"]} 与 snake_case 写法都会被静默忽略，
     * 因此这里既断言"下发了 customData"，也断言"没有多写 customModelData"，
     * 对纯装配函数逐分支断言，不依赖 Bukkit 注册表。
     * */
    private static void verifyCustomDataFields() {
        JsonObject recognized = new JsonObject();
        boolean sent = ItemStackCodec.writeCustomData(recognized, 10017, true,
                CustomDataSupport.MODE_ITEMSADDER, true);
        check("itemsadder 模式 + 已识别 → 下发 item.customData",
                sent && recognized.has("customData") && recognized.get("customData").getAsInt() == 10017);
        check("不再写入 item.customModelData（mvg 不读该字段）", !recognized.has("customModelData"));
        check("customData 不塞进 components", !recognized.has("components"));

        JsonObject skipped = new JsonObject();
        boolean notSent = ItemStackCodec.writeCustomData(skipped, 10017, false,
                CustomDataSupport.MODE_ITEMSADDER, true);
        check("itemsadder 模式 + 未识别 → 不下发 customData（默认行为不变）",
                !notSent && !skipped.has("customData"));
        check("跳过时也不留下 customModelData 之类的字段", skipped.size() == 0);

        JsonObject forced = new JsonObject();
        boolean always = ItemStackCodec.writeCustomData(forced, 10017, false,
                CustomDataSupport.MODE_ALWAYS, true);
        check("always 模式 + 未识别 → 仍然下发 customData",
                always && forced.has("customData") && forced.get("customData").getAsInt() == 10017);
        check("always 模式同样只有 customData 一个字段",
                forced.size() == 1 && !forced.has("customModelData"));

        JsonObject disabled = new JsonObject();
        boolean off = ItemStackCodec.writeCustomData(disabled, 10017, true,
                CustomDataSupport.MODE_ALWAYS, false);
        check("render.itemsadder=false → 不写任何字段",
                !off && disabled.size() == 0);

        JsonObject empty = new JsonObject();
        boolean nothing = ItemStackCodec.writeCustomData(empty, null, true,
                CustomDataSupport.MODE_ALWAYS, true);
        check("没有自定义模型数据时不写任何字段", !nothing && empty.size() == 0);

        JsonObject zero = new JsonObject();
        boolean zeroSkipped = ItemStackCodec.writeCustomData(zero, 0, true,
                CustomDataSupport.MODE_ALWAYS, true);
        check("custom_model_data=0 视为未设置，不写任何字段", !zeroSkipped && zero.size() == 0);

        JsonObject negative = new JsonObject();
        boolean negativeSkipped = ItemStackCodec.writeCustomData(negative, -7, true,
                CustomDataSupport.MODE_ALWAYS, true);
        check("负值（mvg 会拒绝）不写任何字段", !negativeSkipped && negative.size() == 0);
    }

    /**
     * tooltip 行序（附魔 → lore → 属性 → 单值组件）与属性合并显示
     * */
    private static void verifyTooltipLayout() throws IOException {
        JsonArray enchantments = rows("\u00A77锋利 \u00A77X");
        JsonArray lore = rows("\u00A75描述");
        JsonArray attributes = rows("\u00A77在主手时：");
        JsonArray components = rows("\u00A77耐久度: 1549 / 1561");
        JsonArray lines = TooltipBuilder.assembleLines(enchantments, lore, attributes, components);
        check("行序 = 附魔 → lore → 属性 → 单值组件", lines.size() == 4
                && lines.get(0).getAsString().contains("锋利")
                && lines.get(1).getAsString().contains("描述")
                && lines.get(2).getAsString().contains("在主手时")
                && lines.get(3).getAsString().contains("耐久度"));

        ItemNameTable names;
        try (InputStream input = MvgSandboxMain.class.getResourceAsStream("/item-names-zh_cn.json")) {
            names = ItemNameTable.load(input);
        }
        check("内置中文名称表可用", !names.isEmpty());

        check("攻击伤害合并显示基础值 = 8 攻击伤害",
                "8 攻击伤害".equals(TooltipBuilder.attributeLine(
                        "attack_damage", "攻击伤害", 7, "add_value", 1, names)));
        check("攻击速度合并显示基础值 = 1.6 攻击速度",
                "1.6 攻击速度".equals(TooltipBuilder.attributeLine(
                        "attack_speed", "攻击速度", -2.4, "add_value", 4, names)));
        check("非合并属性保持 + 前缀 = +0.75 横扫伤害比率",
                "+0.75 横扫伤害比率".equals(TooltipBuilder.attributeLine(
                        "sweeping_damage_ratio", "横扫伤害比率", 0.75, "add_value", 0, names)));
        check("基础值未知时退化为 +7 攻击伤害",
                "+7 攻击伤害".equals(TooltipBuilder.attributeLine(
                        "attack_damage", "攻击伤害", 7, "add_value", Double.NaN, names)));

        // 十六进制颜色：原文必须是 §x§R§R§G§G§B§B（mvg 侧解析该形式），不能被中间环节吃掉
        String hex = "\u00A7x\u00A76\u00A76\u00A79\u00A79\u00A7F\u00A7F星火";
        check("&#RRGGBB 转成 §x§R§R§G§G§B§B 形式", hex.equals(LegacyText.colorize("&#6699FF星火")));
        check("&x&R&R&G&G&B&B 同样转换", hex.equals(LegacyText.colorize("&x&6&6&9&9&F&F星火")));
        check("hex 颜色码在 strip 后得到纯文本", "星火".equals(LegacyText.strip(hex)));
        check("识别出带样式（保留原文，不会被补默认紫色斜体）", LegacyText.containsStyle(hex));
    }

    /**
     * 皮肤来源指纹：指纹不变复用缓存、指纹变化重新下载、来源失效保留缓存
     * <p>
     * 用本机临时 HTTP 服务提供两张不同的 64×64 皮肤，全程不发外网请求。
     * */
    private static void verifySkinFingerprint(Logger logger) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] classic = resourceBytes("/default-skin-classic.png");
        byte[] slim = resourceBytes("/default-skin-slim.png");
        server.createContext("/a.png", exchange -> writePng(exchange, classic));
        server.createContext("/b.png", exchange -> writePng(exchange, slim));
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            Path folder = OUTPUT.resolve("skin-fingerprint");
            Files.createDirectories(folder);
            SkinCache cache = new SkinCache(folder.toFile(), TimeUnit.DAYS.toMillis(7),
                    resource("/default-skin-classic.png"), resource("/default-skin-slim.png"), logger);

            AtomicReference<String> url = new AtomicReference<>(base + "/a.png");
            SkinSource stub = new StubSource(() -> new TextureRef(url.get(), SkinData.MODEL_CLASSIC, "stub"));
            SkinResolver resolver = new SkinResolver(cache, 256 * 1024, 5, logger, null, true);
            resolver.addSource(stub);

            UUID uuid = UUID.randomUUID();
            SkinData first = resolver.resolve(uuid, "Steve", false);
            check("stub 来源解析出真实皮肤", !first.isDefault());
            String firstUri = first.getDataUri();

            SkinData reused = resolver.resolve(uuid, "Steve", false);
            check("指纹未变时复用缓存", reused.getDataUri().equals(firstUri));

            url.set(base + "/b.png");
            SkinData refreshed = resolver.resolve(uuid, "Steve", false);
            check("指纹变化（换肤）后重新下载", !refreshed.getDataUri().equals(firstUri));
            check("新皮肤可从缓存回读（主线程路径）",
                    resolver.getCachedOrDefault(uuid, null).getDataUri().equals(refreshed.getDataUri()));

            SkinResolver brokenResolver = new SkinResolver(cache, 256 * 1024, 5, logger, null, true);
            brokenResolver.addSource(new StubSource(null));
            SkinData kept = brokenResolver.resolve(uuid, "Steve", false);
            check("来源不可用时保留既有缓存而不是退回默认皮肤",
                    !kept.isDefault() && kept.getDataUri().equals(refreshed.getDataUri()));
        } finally {
            server.stop(0);
        }
    }

    private static JsonArray rows(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static void writePng(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "image/png");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static byte[] resourceBytes(String name) throws IOException {
        try (InputStream input = resource(name)) {
            return input.readAllBytes();
        }
    }

    /**
     * 注入式皮肤来源：返回给定贴图引用，或返回 {@code null} 表示来源不可用
     * */
    private static final class StubSource implements SkinSource {

        private final java.util.function.Supplier<TextureRef> ref;

        private StubSource(java.util.function.Supplier<TextureRef> ref) {
            this.ref = ref;
        }

        @Override
        public String name() {
            return "stub";
        }

        @Override
        public SkinLookup lookup(UUID uuid, String playerName) {
            TextureRef value = ref == null ? null : ref.get();
            return value == null ? SkinLookup.miss("stub 不可用") : SkinLookup.hit(value);
        }

    }

    private static JsonObject tooltipData(JsonArray lore) {        JsonObject data = new JsonObject();
        data.addProperty("title", "\u00A7b钻石剑");
        JsonObject item = new JsonObject();
        item.addProperty("id", "minecraft:diamond_sword");
        item.addProperty("count", 1);
        if (lore.size() > 0) {
            item.add("lore", lore);
        }
        data.add("item", item);
        return data;
    }

    // ------------------------------------------------------------------ 基础设施

    /**
     * 记录每一次真正发出去的报文，便于落盘为可复跑的测试 JSON
     * */
    private static final class RecordingClient extends MvgClient {

        private JsonObject lastPayload;

        RecordingClient(MvgConfig config, Logger logger) {
            super(config.baseUrl, config.apiKey, config.connectTimeoutSeconds,
                    config.requestTimeoutSeconds, logger);
        }

        @Override
        public MvgRenderResult render(JsonObject payload) throws MvgApiException {
            this.lastPayload = payload.deepCopy();
            return super.render(payload);
        }

        JsonObject lastPayload() {
            return lastPayload == null ? new JsonObject() : lastPayload;
        }

    }

    private static void save(String name, JsonObject payload, MvgRenderResult result) throws IOException {
        String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(payload);
        Files.write(OUTPUT.resolve(name + ".json"), json.getBytes(StandardCharsets.UTF_8));
        if (result == null) {
            System.out.println("  SKIP 无可渲染数据: " + name);
            return;
        }
        byte[] bytes = Base64.getDecoder().decode(result.getData());
        Files.write(OUTPUT.resolve(name + ".png"), bytes);
        System.out.println(String.format("  OK %dx%d, %d bytes -> %s.png / %s.json",
                result.getWidth(), result.getHeight(), bytes.length, name, name));
    }

    private static void expectSize(String what, MvgRenderResult result, int width, int height) {
        boolean ok = result != null && result.getWidth() == width && result.getHeight() == height;
        check(what + "（实际 " + (result == null ? "null" : result.getWidth() + "x" + result.getHeight()) + "）", ok);
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            PASSED.add(what);
            System.out.println("  PASS " + what);
        } else {
            FAILED.add(what);
            System.out.println("  FAIL " + what);
        }
    }

    private static void expectRejected(String what, ThrowingRunnable action) {
        try {
            action.run();
            check(what, false);
        } catch (MvgApiException e) {
            check(what + "（" + e.getCode() + "）", true);
        }
    }

    private static boolean sameImage(MvgRenderResult left, MvgRenderResult right) {
        if (left == null || right == null) {
            return false;
        }
        try {
            BufferedImage a = decode(left);
            BufferedImage b = decode(right);
            if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
                return false;
            }
            for (int y = 0; y < a.getHeight(); y++) {
                for (int x = 0; x < a.getWidth(); x++) {
                    if (a.getRGB(x, y) != b.getRGB(x, y)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static BufferedImage decode(MvgRenderResult result) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(result.getData())));
    }

    private static int resolvedCustomItems(RecordingClient client) {
        try {
            JsonObject sources = client.sources();
            JsonObject items = sources.getAsJsonObject("customContents").getAsJsonObject("items");
            return items.get("resolved").getAsInt();
        } catch (RuntimeException | MvgApiException e) {
            return -1;
        }
    }

    private static SkinCache createSkinCache(Logger logger) throws IOException {
        Path folder = OUTPUT.resolve("skin-cache");
        Files.createDirectories(folder);
        return new SkinCache(folder.toFile(), TimeUnit.DAYS.toMillis(7),
                resource("/default-skin-classic.png"), resource("/default-skin-slim.png"), logger);
    }

    private static InputStream resource(String name) {
        InputStream input = MvgSandboxMain.class.getResourceAsStream(name);
        if (input == null) {
            throw new IllegalStateException("内置资源缺失: " + name);
        }
        return input;
    }

    private static String defaultDataUri() throws IOException {
        try (InputStream input = resource("/default-skin-classic.png")) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(input.readAllBytes());
        }
    }

    private static void ensureResources(MvgClient client) throws MvgApiException {
        System.out.println("=== 资源状态 ===");
        MvgResourceState state = client.resourceState(MvgClient.SUPPORTED_VERSION);
        System.out.println("  " + state);
        if (state == null || !state.isReady()) {
            System.out.println("  资源尚未就绪，先请求准备 ...");
            state = client.prepareResource(MvgClient.SUPPORTED_VERSION);
            System.out.println("  " + state);
        }
        if (state == null || !state.isReady()) {
            throw new IllegalStateException("mvg 资源未就绪，无法验证渲染");
        }
    }

    private static MvgConfig sandboxConfig(String baseUrl) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("mvg.base-url", baseUrl);
        configuration.set("mvg.api-key", "");
        configuration.set("mvg.connect-timeout-seconds", 5);
        configuration.set("mvg.request-timeout-seconds", 60);
        configuration.set("render.inventory-title", "Steve 的背包");
        configuration.set("render.enderchest-title", "Steve 的末影箱");
        configuration.set("render.playerlist-header", "&a服务器在线 %total% 人");
        configuration.set("render.playerlist-footer", "&7%servers%");
        configuration.set("render.scale.inventory", SCALE);
        configuration.set("render.scale.enderchest", SCALE);
        configuration.set("render.scale.tooltip", SCALE);
        configuration.set("render.scale.playerlist", SCALE);
        return new MvgConfig(configuration);
    }

    private static PlayerSnapshotPayload inventoryPayload(String skin) {
        PlayerSnapshotPayload payload = new PlayerSnapshotPayload();
        payload.setFound(true);
        payload.setPlayerName("Steve");
        payload.setServerName("demo");
        payload.setSkinDataUri(skin);
        payload.setSkinModel("classic");

        JsonArray slots = new JsonArray();
        // 背包区：0–26
        slots.add(slot(0, item("minecraft:diamond_sword", 1)));
        slots.add(slot(9, item("minecraft:golden_apple", 16)));
        slots.add(slot(20, item("minecraft:stone", 64)));
        // 快捷栏：27–35
        slots.add(slot(27, item("minecraft:bow", 1)));
        payload.setSlots(slots);

        JsonArray armor = new JsonArray();
        armor.add(slot(0, item("minecraft:diamond_helmet", 1)));
        armor.add(emptySlot(1));
        armor.add(slot(2, item("minecraft:iron_leggings", 1)));
        armor.add(emptySlot(3));
        payload.setArmor(armor);

        payload.setMainHand(item("minecraft:diamond_sword", 1));
        payload.setOffHand(item("minecraft:golden_apple", 1));
        return payload;
    }

    private static PlayerSnapshotPayload enderChestPayload() {
        PlayerSnapshotPayload payload = new PlayerSnapshotPayload();
        payload.setFound(true);
        payload.setPlayerName("Steve");
        JsonArray slots = new JsonArray();
        slots.add(slot(0, item("minecraft:ender_pearl", 16)));
        slots.add(slot(13, item("minecraft:diamond", 32)));
        payload.setSlots(slots);
        return payload;
    }

    private static List<OnlinePlayerEntry> onlinePlayers(String skin) {
        List<OnlinePlayerEntry> players = new ArrayList<>();
        players.add(entry("Steve", "demo-1", 42, "survival", skin));
        players.add(entry("Alex", "demo-1", 128, "creative", skin));
        players.add(entry("Herobrine", "demo-2", 850, "adventure", skin));
        return players;
    }

    private static OnlinePlayerEntry entry(String name, String server, int ping, String mode, String skin) {
        OnlinePlayerEntry entry = new OnlinePlayerEntry();
        entry.setName(name);
        entry.setDisplay(name);
        entry.setServerName(server);
        entry.setPing(ping);
        entry.setGameMode(mode);
        entry.setTexture(skin);
        return entry;
    }

    private static JsonObject item(String id, int count) {
        JsonObject item = new JsonObject();
        item.addProperty("id", id);
        item.addProperty("count", count);
        return item;
    }

    private static JsonObject slot(int index, JsonObject item) {
        JsonObject slot = new JsonObject();
        slot.addProperty("slot", index);
        slot.add("item", item);
        return slot;
    }

    private static JsonObject emptySlot(int index) {
        JsonObject slot = new JsonObject();
        slot.addProperty("slot", index);
        slot.addProperty("empty", true);
        return slot;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws MvgApiException;
    }

}
