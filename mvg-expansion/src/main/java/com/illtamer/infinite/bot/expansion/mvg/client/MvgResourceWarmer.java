package com.illtamer.infinite.bot.expansion.mvg.client;

import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * mvg 资源预热器
 * <p>
 * 附属启动时向 mvg 发送准备当前服务器版本资源的请求，并轮询直到资源就绪；
 * 只有预热成功（{@link #isReady()}）后附属才对外提供渲染能力。
 * <p>
 * 轮询采用"一次 HTTP 请求 + 延时调度下一次"的方式，每次只短暂占用线程池，
 * 不会长时间独占 {@link BotScheduler#IO_INTENSIVE} 的线程。
 * */
public class MvgResourceWarmer {

    public enum State {
        /** 尚未开始 */
        IDLE,
        /** 正在准备资源 */
        PREPARING,
        /** 资源就绪，可以渲染 */
        READY,
        /** 已按配置跳过预热，直接转发渲染请求 */
        SKIPPED,
        /** 准备失败且不再重试，或附属已卸载 */
        FAILED
    }

    private final MvgClient client;
    private final Logger logger;
    private final String version;
    private final int pollIntervalSeconds;
    private final long timeoutMillis;
    private final int retryDelaySeconds;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile State state = State.IDLE;
    private volatile MvgResourceState lastResourceState;
    private volatile String lastError = "";
    private volatile long startedAt;
    private volatile long finishedAt;

    public MvgResourceWarmer(MvgClient client, String version, int pollIntervalSeconds,
                             int timeoutMinutes, int retryDelaySeconds, Logger logger) {
        this.client = client;
        this.version = version;
        this.pollIntervalSeconds = Math.max(1, pollIntervalSeconds);
        this.timeoutMillis = Math.max(1L, timeoutMinutes) * 60_000L;
        this.retryDelaySeconds = retryDelaySeconds;
        this.logger = logger;
    }

    public State getState() {
        return state;
    }

    /**
     * 是否可以对外提供渲染：资源已就绪，或配置显式跳过了预热
     * */
    public boolean isReady() {
        return state == State.READY || state == State.SKIPPED;
    }

    /**
     * 按配置跳过预热（{@code warmup.enabled=false}）
     * <p>
     * 此时直接把请求交给 mvg，由 mvg 自己决定资源是否可用
     * */
    public void bypass() {
        running.set(false);
        state = State.SKIPPED;
        lastError = "";
        logger.info("[MvgWarmup] 已按配置跳过资源预热，渲染请求将直接转发给 mvg");
    }

    public MvgResourceState getLastResourceState() {
        return lastResourceState;
    }

    public String getLastError() {
        return lastError;
    }

    public String getVersion() {
        return version;
    }

    /**
     * 启动预热流程（异步，不阻塞调用方）
     * */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            logger.info("[MvgWarmup] 预热流程已在运行中，忽略重复启动");
            return;
        }
        state = State.PREPARING;
        startedAt = System.currentTimeMillis();
        finishedAt = 0L;
        lastError = "";
        logger.info(String.format("[MvgWarmup] 开始预热 Minecraft %s 资源 (mvg: %s)", version, client.getBaseUrl()));
        BotScheduler.runTask(this::prepare);
    }

    /**
     * 停止预热（附属卸载时调用）：正在排队的轮询任务会在执行前自行退出
     * */
    public void stop() {
        running.set(false);
        state = State.FAILED;
        lastError = "附属已卸载";
    }

    /**
     * 供玩家侧展示的简短状态描述
     * */
    public String describeState() {
        switch (state) {
            case READY:
                return "已就绪";
            case SKIPPED:
                return "已跳过预热";
            case PREPARING:
                MvgResourceState resource = lastResourceState;
                return resource == null ? "准备中" : "准备中(" + resource.describe() + ")";
            case FAILED:
                return lastError.isEmpty() ? "准备失败" : "准备失败(" + lastError + ")";
            default:
                return "未开始";
        }
    }

    private void prepare() {
        try {
            if (!client.isHealthy()) {
                fail("mvg 服务未返回版本列表（GET /api/v1/versions）", true);
                return;
            }
        } catch (MvgApiException e) {
            fail("无法连接 mvg 服务: " + e.getMessage(), true);
            return;
        }
        poll();
    }

    private void poll() {
        if (!running.get()) {
            return;
        }
        try {
            MvgResourceState resource = lastResourceState == null
                    ? client.prepareResource(version)
                    : client.resourceState(version);
            lastResourceState = resource;
            if (resource.isReady()) {
                succeed(resource);
                return;
            }
            if (resource.isFailed()) {
                fail(resource.getError().isEmpty() ? "mvg 资源准备失败" : resource.getError(), true);
                return;
            }
            if (System.currentTimeMillis() - startedAt > timeoutMillis) {
                fail("等待 mvg 资源准备超时（>" + (timeoutMillis / 60_000L) + " 分钟，当前进度 "
                        + resource.describe() + "）", true);
                return;
            }
            if (!resource.getWarnings().isEmpty()) {
                logger.warn("[MvgWarmup] mvg 资源准备警告: " + String.join("; ", resource.getWarnings()));
            }
            logger.info("[MvgWarmup] 资源准备中: " + resource.describe());
            scheduleNextPoll();
        } catch (MvgApiException e) {
            fail("查询 mvg 资源状态失败(" + e.getCode() + "): " + e.getMessage(), true);
        }
    }

    private void scheduleNextPoll() {
        BotScheduler.runTaskLater(() -> {
            if (state == State.READY || !running.get()) {
                return;
            }
            BotScheduler.runTask(this::poll);
        }, pollIntervalSeconds);
    }

    private void succeed(MvgResourceState resource) {
        state = State.READY;
        finishedAt = System.currentTimeMillis();
        running.set(false);
        long seconds = Math.max(0L, (finishedAt - startedAt) / 1000L);
        logger.info(String.format("[MvgWarmup] Minecraft %s 资源已就绪，用时 %d 秒，渲染功能已可用",
                version, seconds));
        if (!resource.getWarnings().isEmpty()) {
            logger.warn("[MvgWarmup] 资源准备警告: " + String.join("; ", resource.getWarnings()));
        }
    }

    private void fail(String message, boolean retryable) {
        lastError = message;
        if (retryable && retryDelaySeconds > 0) {
            state = State.PREPARING;
            logger.warn(String.format("[MvgWarmup] %s，%d 秒后重试", message, retryDelaySeconds));
            BotScheduler.runTaskLater(() -> {
                if (state == State.READY || !running.get()) {
                    return;
                }
                startedAt = System.currentTimeMillis();
                lastResourceState = null;
                BotScheduler.runTask(this::prepare);
            }, retryDelaySeconds);
            return;
        }
        state = State.FAILED;
        finishedAt = System.currentTimeMillis();
        running.set(false);
        logger.error("[MvgWarmup] 预热失败: " + message);
    }

}
