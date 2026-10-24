package com.illtamer.infinite.bot.expansion.mvg.listener;

import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.expansion.mvg.MvgExpansion;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgApiException;
import com.illtamer.infinite.bot.expansion.mvg.client.MvgRenderResult;
import com.illtamer.infinite.bot.expansion.mvg.config.MvgConfig;
import com.illtamer.infinite.bot.expansion.mvg.distribute.MvgOnlineQueryListener;
import com.illtamer.infinite.bot.expansion.mvg.distribute.MvgPlayerQueryListener;
import com.illtamer.infinite.bot.expansion.mvg.distribute.OnlinePlayerEntry;
import com.illtamer.infinite.bot.expansion.mvg.distribute.OnlinePlayersPayload;
import com.illtamer.infinite.bot.expansion.mvg.distribute.PlayerSnapshotPayload;
import com.illtamer.infinite.bot.expansion.mvg.render.MvgRenderService;
import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import com.illtamer.infinite.bot.minecraft.api.StaticAPI;
import com.illtamer.infinite.bot.minecraft.api.distribute.DistributedEventContext;
import com.illtamer.infinite.bot.minecraft.api.event.EventHandler;
import com.illtamer.infinite.bot.minecraft.api.event.EventPriority;
import com.illtamer.infinite.bot.minecraft.api.event.Listener;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import com.illtamer.infinite.bot.minecraft.pojo.PlayerData;
import com.illtamer.perpetua.sdk.event.message.GroupMessageEvent;
import com.illtamer.perpetua.sdk.event.message.MessageEvent;
import com.illtamer.perpetua.sdk.event.message.PrivateMessageEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QQ 消息关键词入口
 * <p>
 * 监听群消息与私聊消息，命中配置中的关键词后拦截消息并在异步线程完成
 * "跨服采集 → 调用 mvg 渲染 → 回图" 的全流程。
 * 只有主节点（{@link StaticAPI#isMaster()}）会响应，避免群组服下多节点重复回复。
 * */
public class MvgKeywordListener implements Listener {

    private enum Action {
        INVENTORY, MAIN_HAND, OFF_HAND, ENDER_CHEST, ONLINE
    }

    /** 冷却记录的最大条目数，超过后清理过期项 */
    private static final int COOLDOWN_MAP_LIMIT = 512;

    private final MvgExpansion expansion;
    private final MvgConfig config;
    private final Language language;
    private final MvgRenderService renderService;
    private final MvgPlayerQueryListener playerQuery;
    private final MvgOnlineQueryListener onlineQuery;
    private final ImageSender imageSender;
    private final Logger logger;

    private final Map<Long, Long> lastRequest = new ConcurrentHashMap<>();

    public MvgKeywordListener(MvgExpansion expansion, MvgConfig config, Language language,
                              MvgRenderService renderService, MvgPlayerQueryListener playerQuery,
                              MvgOnlineQueryListener onlineQuery, ImageSender imageSender) {
        this.expansion = expansion;
        this.config = config;
        this.language = language;
        this.renderService = renderService;
        this.playerQuery = playerQuery;
        this.onlineQuery = onlineQuery;
        this.imageSender = imageSender;
        this.logger = expansion.getLogger();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGroupMessage(GroupMessageEvent event) {
        handle(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrivateMessage(PrivateMessageEvent event) {
        handle(event);
    }

    private void handle(MessageEvent event) {
        if (!StaticAPI.isMaster()) {
            return;
        }
        String raw = event.getRawMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        final Request request = parse(raw.trim());
        if (request == null) {
            return;
        }
        event.setCancelled(true);
        final long userId = event.getUserId() == null ? 0L : event.getUserId();
        BotScheduler.runTask(() -> execute(event, request, userId));
    }

    // ------------------------------------------------------------------ 关键词解析

    private Request parse(String raw) {
        Request request = match(raw, config.keywordInventory, Action.INVENTORY);
        if (request != null) {
            return request;
        }
        request = match(raw, config.keywordMainHand, Action.MAIN_HAND);
        if (request != null) {
            return request;
        }
        request = match(raw, config.keywordOffHand, Action.OFF_HAND);
        if (request != null) {
            return request;
        }
        request = match(raw, config.keywordEnderChest, Action.ENDER_CHEST);
        if (request != null) {
            return request;
        }
        request = match(raw, config.keywordOnline, Action.ONLINE);
        // 在线列表始终是全服数据，不接受"指定玩家"参数，
        // 避免与其它关键词"可跟玩家名"的语义混淆
        return request == null || request.argument != null ? null : request;
    }

    /**
     * 关键词全等匹配，或"关键词 + 空白 + 玩家名"形式的前缀匹配
     * */
    private Request match(String raw, String keyword, Action action) {
        if (!MvgConfig.enabled(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        if (raw.equals(trimmed)) {
            return new Request(action, null);
        }
        if (!raw.startsWith(trimmed) || raw.length() == trimmed.length()) {
            return null;
        }
        char separator = raw.charAt(trimmed.length());
        if (separator != ' ' && separator != '\u3000' && separator != '\t') {
            return null;
        }
        String argument = raw.substring(trimmed.length()).trim();
        return argument.isEmpty() ? null : new Request(action, argument);
    }

    // ------------------------------------------------------------------ 执行

    private void execute(MessageEvent event, Request request, long userId) {
        try {
            // 附属已卸载：不再消耗资源回复，避免关闭过程中继续发起请求
            if (expansion.isDisabled()) {
                return;
            }
            if (!acceptCooldown(userId)) {
                reply(event, language.get("key-word", "cooldown")
                        .replace("%seconds%", Integer.toString(config.cooldownSeconds)));
                return;
            }
            if (request.action == Action.ONLINE) {
                doOnline(event);
                return;
            }
            Target target = resolveTarget(event, request, userId);
            if (target == null) {
                return;
            }
            if (!renderService.isReady()) {
                reply(event, language.get("key-word", "warming-up")
                        .replace("%state%", expansion.describeWarmupState()));
                return;
            }
            switch (request.action) {
                case INVENTORY:
                    doInventory(event, target);
                    break;
                case ENDER_CHEST:
                    doEnderChest(event, target);
                    break;
                case MAIN_HAND:
                    doHand(event, target, PlayerSnapshotPayload.HAND_MAIN);
                    break;
                case OFF_HAND:
                    doHand(event, target, PlayerSnapshotPayload.HAND_OFF);
                    break;
                default:
                    break;
            }
        } catch (MvgApiException e) {
            logger.warn("[Render] 渲染调用失败: " + e);
            reply(event, language.get("key-word", "render-failed").replace("%reason%", describe(e)));
        } catch (IOException e) {
            logger.warn("[Render] 图片发送失败", e);
            reply(event, language.get("key-word", "image-failed"));
        } catch (RuntimeException e) {
            logger.error("[Render] 处理请求异常", e);
            reply(event, language.get("key-word", "error"));
        }
    }

    private void doInventory(MessageEvent event, Target target) throws MvgApiException, IOException {
        PlayerSnapshotPayload payload = querySnapshot(event, target,
                PlayerSnapshotPayload.ACTION_INVENTORY, null, true);
        if (payload == null) {
            return;
        }
        sendImage(event, renderService.renderInventory(payload));
    }

    private void doEnderChest(MessageEvent event, Target target) throws MvgApiException, IOException {
        PlayerSnapshotPayload payload = querySnapshot(event, target,
                PlayerSnapshotPayload.ACTION_ENDER_CHEST, null, false);
        if (payload == null) {
            return;
        }
        sendImage(event, renderService.renderEnderChest(payload));
    }

    private void doHand(MessageEvent event, Target target, String hand) throws MvgApiException, IOException {
        PlayerSnapshotPayload payload = querySnapshot(event, target,
                PlayerSnapshotPayload.ACTION_HAND, hand, false);
        if (payload == null) {
            return;
        }
        boolean main = PlayerSnapshotPayload.HAND_MAIN.equals(hand);
        JsonObject tooltip = main ? payload.getMainHandTooltip() : payload.getOffHandTooltip();
        MvgRenderResult result = renderService.renderHandTooltip(tooltip);
        if (result == null) {
            reply(event, language.get("key-word", "empty-hand")
                    .replace("%hand%", language.get("hand", main ? "main" : "off")));
            return;
        }
        sendImage(event, result);
    }

    private void doOnline(MessageEvent event) throws MvgApiException, IOException {
        if (!renderService.isReady()) {
            reply(event, language.get("key-word", "warming-up")
                    .replace("%state%", expansion.describeWarmupState()));
            return;
        }
        final DistributedEventContext context = new DistributedEventContext();
        context.setParam(MvgOnlineQueryListener.PARAM_INCLUDE_SKIN, config.onlineIncludeSkin);
        context.setParam(MvgOnlineQueryListener.PARAM_MAX, config.playerListMaxPlayers);

        final List<OnlinePlayerEntry>[] holder = newHolder();
        final boolean[] unreachable = new boolean[1];
        final boolean[] truncated = new boolean[1];
        final boolean[] failed = new boolean[1];
        try {
            onlineQuery.getProcessor().tryProcessEvent(MvgOnlineQueryListener.IDENTIFIER, context,
                    result -> {
                        List<OnlinePlayerEntry> entries = new ArrayList<>();
                        for (OnlinePlayersPayload payload : result.getDataList()) {
                            if (payload == null) {
                                continue;
                            }
                            if (payload.getPlayers() != null) {
                                entries.addAll(payload.getPlayers());
                            }
                            if (payload.isTruncated()
                                    || (payload.getError() != null && !payload.getError().isEmpty())) {
                                truncated[0] = true;
                            }
                        }
                        if (!result.getFailedClientList().isEmpty()) {
                            unreachable[0] = true;
                        }
                        holder[0] = entries;
                    },
                    error -> {
                        logger.warn("[Distribute] 在线玩家查询失败", error);
                        failed[0] = true;
                    });
        } catch (Throwable e) {
            // 本体 API 不兼容等情况会让整个分布式调用直接抛出，此时必须退化为单机查询
            logger.warn("[Distribute] 分布式在线查询不可用，回退单机查询", e);
            failed[0] = true;
        }

        List<OnlinePlayerEntry> players;
        final boolean localOnly = failed[0];
        if (localOnly) {
            OnlinePlayersPayload local = onlineQuery.queryLocal(context);
            players = local == null || local.getPlayers() == null
                    ? new ArrayList<>() : new ArrayList<>(local.getPlayers());
        } else {
            players = holder[0] == null ? new ArrayList<>() : holder[0];
        }
        if (players.isEmpty()) {
            // 跨服链路整体不可用时，本服列表为空并不能说明"全服无人"
            reply(event, language.get("key-word", localOnly ? "query-failed" : "no-online-player"));
            return;
        }
        if (players.size() > config.playerListMaxPlayers) {
            players = new ArrayList<>(players.subList(0, config.playerListMaxPlayers));
            truncated[0] = true;
        }
        sendImage(event, renderService.renderPlayerList(players));
        if (localOnly) {
            reply(event, language.get("key-word", "online-local-fallback")
                    .replace("%server%", expansion.getServerName()));
        } else if (truncated[0] || unreachable[0]) {
            reply(event, language.get("key-word", "online-partial"));
        }
    }

    /**
     * 通过分布式广播在群组服内定位玩家并采集实时数据
     * <p>
     * 其他子服不可达（未响应、跨服链路异常）时回退为只采集本服数据，
     * 并在回退发生时如实告知玩家数据范围。
     *
     * @return 采集结果；玩家不在线、跨服失败或已回复错误提示时返回 {@code null}
     * */
    private PlayerSnapshotPayload querySnapshot(MessageEvent event, Target target, String action,
                                                String hand, boolean needSkin) {
        final DistributedEventContext context = new DistributedEventContext();
        context.setParam(MvgPlayerQueryListener.PARAM_ACTION, action);
        context.setParam(MvgPlayerQueryListener.PARAM_NEED_SKIN, needSkin);
        if (hand != null) {
            context.setParam(MvgPlayerQueryListener.PARAM_HAND, hand);
        }
        if (target.uuid != null) {
            context.setParam(MvgPlayerQueryListener.PARAM_UUID, target.uuid);
        }
        if (target.validUuid != null) {
            context.setParam(MvgPlayerQueryListener.PARAM_VALID_UUID, target.validUuid);
        }
        if (target.name != null) {
            context.setParam(MvgPlayerQueryListener.PARAM_NAME, target.name);
        }

        final PlayerSnapshotPayload[] holder = new PlayerSnapshotPayload[1];
        final int[] unreachableClients = new int[1];
        final boolean[] failed = new boolean[1];
        try {
            playerQuery.getProcessor().tryProcessEvent(MvgPlayerQueryListener.IDENTIFIER, context,
                    result -> {
                        // 子服超时不会走 errorConsumer，只会体现在 failedClientList 里
                        unreachableClients[0] = result.getFailedClientList().size();
                        for (PlayerSnapshotPayload payload : result.getDataList()) {
                            if (payload != null && payload.isFound()) {
                                holder[0] = payload;
                                return;
                            }
                        }
                    },
                    error -> {
                        logger.warn("[Distribute] 玩家数据查询失败", error);
                        failed[0] = true;
                    });
        } catch (Throwable e) {
            // 本体 API 不兼容等情况会让整个分布式调用直接抛出，此时必须退化为单机查询
            logger.warn("[Distribute] 分布式玩家查询不可用，回退单机查询", e);
            failed[0] = true;
        }
        if (holder[0] != null) {
            return holder[0];
        }

        // 跨服未命中：回退到本服采集，保证单机服 / 跨服不可用时功能依然可用
        final boolean degraded = failed[0] || unreachableClients[0] > 0;
        PlayerSnapshotPayload local = playerQuery.queryLocal(context);
        if (local != null && local.isFound()) {
            if (degraded) {
                reply(event, language.get("key-word", "local-fallback")
                        .replace("%server%", local.getServerName() == null
                                ? expansion.getServerName() : local.getServerName()));
            }
            return local;
        }
        if (local != null && local.getError() != null && !local.getError().isEmpty()) {
            logger.warn("[Distribute] 本服采集失败: " + local.getError());
            reply(event, language.get("key-word", "query-failed"));
            return null;
        }
        if (degraded) {
            // 有子服没响应时，"不在线"的结论并不可靠，如实告知查询不完整
            reply(event, language.get("key-word", "query-failed"));
        } else {
            reply(event, language.get("key-word", "player-offline")
                    .replace("%player%", describeName(target)));
        }
        return null;
    }

    // ------------------------------------------------------------------ 目标解析

    /**
     * 解析查询目标：默认取发送者自身绑定，管理员可额外指定玩家名
     *
     * @return 目标玩家；已回复错误提示时返回 {@code null}
     * */
    private Target resolveTarget(MessageEvent event, Request request, long userId) {
        if (request.argument != null) {
            if (!StaticAPI.isAdmin(userId)) {
                reply(event, language.get("key-word", "no-permission"));
                return null;
            }
            return new Target(null, null, request.argument, request.argument);
        }
        PlayerData data = StaticAPI.getRepository().queryByUserId(userId);
        if (data == null || (data.getUuid() == null && data.getValidUUID() == null)) {
            reply(event, language.get("key-word", "not-bound"));
            return null;
        }
        return new Target(data.getUuid(), data.getValidUUID(), null, null);
    }

    private String describeName(Target target) {
        if (target.name != null) {
            return target.name;
        }
        if (target.validUuid != null) {
            return target.validUuid;
        }
        return target.uuid == null ? "" : target.uuid;
    }

    // ------------------------------------------------------------------ 输出

    private void sendImage(MessageEvent event, MvgRenderResult result) throws IOException {
        if (result == null) {
            return;
        }
        event.reply(imageSender.build(result));
    }

    private void reply(MessageEvent event, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            event.reply(text);
        } catch (RuntimeException e) {
            logger.warn("[Render] 回复消息失败", e);
        }
    }

    /**
     * 把 mvg 的错误码翻译为玩家可读的原因
     * */
    private String describe(MvgApiException e) {
        // 先按错误码判断：预热中/资源未就绪虽然以异常形式抛出，但并不是"服务不可用"
        switch (e.getCode()) {
            case "warming_up":
                return language.get("key-word", "warming-up")
                        .replace("%state%", expansion.describeWarmupState());
            case "resources_not_ready":
                return language.get("key-word", "resource-not-ready");
            case "unauthorized":
                return language.get("key-word", "unauthorized");
            case "body_too_large":
            case "image_too_large":
                return language.get("key-word", "image-too-large");
            default:
                break;
        }
        if (e.isNetworkError()) {
            return language.get("key-word", "backend-unavailable");
        }
        return e.getMessage();
    }

    private boolean acceptCooldown(long userId) {
        if (config.cooldownSeconds <= 0 || userId == 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        long interval = config.cooldownSeconds * 1000L;
        Long last = lastRequest.get(userId);
        if (last != null && now - last < interval) {
            return false;
        }
        if (lastRequest.size() > COOLDOWN_MAP_LIMIT) {
            lastRequest.entrySet().removeIf(entry -> now - entry.getValue() > interval);
        }
        lastRequest.put(userId, now);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<OnlinePlayerEntry>[] newHolder() {
        return new List[1];
    }

    private static class Request {

        final Action action;

        /** 管理员指定的玩家名，未指定时为 {@code null} */
        final String argument;

        Request(Action action, String argument) {
            this.action = action;
            this.argument = argument;
        }

    }

    private static class Target {

        final String uuid;

        final String validUuid;

        /** 管理员指定的玩家名 */
        final String name;

        final String displayName;

        Target(String uuid, String validUuid, String name, String displayName) {
            this.uuid = uuid;
            this.validUuid = validUuid;
            this.name = name;
            this.displayName = displayName;
        }

    }

}
