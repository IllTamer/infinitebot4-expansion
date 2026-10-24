package com.illtamer.infinite.bot.expansion.mvg.util;

import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 跨 ib4 版本的主线程同步执行桥
 * <p>
 * ib4 4.2.0 起才提供 {@code MinecraftScheduler}（同时兼容 Folia），
 * 而在其之前的运行环境中该类并不存在。若直接编译期引用它，
 * 附属在旧版本体上会在调用点抛出 {@link NoClassDefFoundError}
 * —— 由于该错误发生在分布式广播的接收线程里，旧版本体的广播处理没有兜底，
 * 会导致回包永远发不出去（主节点只能等到 3 秒超时，且附件无任何响应）。
 * <p>
 * 因此这里统一用反射探测 {@code MinecraftScheduler}，探测失败则退回
 * 传统 Bukkit 主线程调度器，保证附属在 4.1.x / 4.2.x 上都能采集实时数据。
 * */
public final class MainThreadCaller {

    /** ib4 4.2.0 新增的主线程调度门面（含 Folia 支持） */
    private static final String SCHEDULER_CLASS =
            "com.illtamer.infinite.bot.minecraft.api.scheduler.MinecraftScheduler";

    /** 等待主线程返回结果的上限，兜底避免调用线程被永久挂住 */
    private static final long WAIT_SECONDS = 10L;

    private static final Method CALL_SYNC_GLOBAL = resolveCallSyncGlobal();

    private MainThreadCaller() {}

    /**
     * 在服务器主线程同步执行任务并取回结果
     * <p>
     * 调用方必须位于异步线程（HTTP / 事件处理等），当前已在主线程时直接内联执行。
     *
     * @throws IllegalStateException 调度失败或等待超时；任务内部抛出的异常按原样（包装为 RuntimeException）抛出
     * */
    public static <T> T call(Callable<T> task) {
        if (Bukkit.isPrimaryThread()) {
            return invoke(task);
        }
        if (CALL_SYNC_GLOBAL != null) {
            try {
                return cast(CALL_SYNC_GLOBAL.invoke(null, task));
            } catch (InvocationTargetException e) {
                throw rethrow(e.getTargetException());
            } catch (IllegalAccessException | RuntimeException e) {
                // 本体升级后签名变更等情况下继续降级，而不是让功能直接不可用
            }
        }
        return callViaBukkitScheduler(task);
    }

    /**
     * 传统主线程调度（Paper / Purpur / Spigot）
     * */
    private static <T> T callViaBukkitScheduler(Callable<T> task) {
        final Plugin plugin = BukkitBootstrap.getInstance();
        if (plugin == null) {
            throw new IllegalStateException("无法获取 InfiniteBot4 插件实例，无法在主线程执行任务");
        }
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin, task).get(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待主线程执行被中断", e);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause() == null ? e : e.getCause();
            throw rethrow(cause);
        } catch (TimeoutException e) {
            throw new IllegalStateException("等待主线程执行超时(" + WAIT_SECONDS + " 秒)", e);
        } catch (UnsupportedOperationException e) {
            // Folia 没有传统调度器，只有 ib4 4.2+ 的 MinecraftScheduler 才可用
            throw new IllegalStateException(
                    "当前服务端不支持传统主线程调度，请将 InfiniteBot4 升级到 4.2.0 及以上版本", e);
        }
    }

    private static Method resolveCallSyncGlobal() {
        try {
            final Class<?> scheduler = Class.forName(SCHEDULER_CLASS);
            return scheduler.getMethod("callSyncGlobal", Callable.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // 旧版本体：按传统调度器处理
            return null;
        }
    }

    private static <T> T invoke(Callable<T> task) {
        try {
            return task.call();
        } catch (Exception e) {
            throw new IllegalStateException("主线程任务执行失败", e);
        }
    }

    private static RuntimeException rethrow(Throwable cause) {
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        return new IllegalStateException("主线程任务执行失败", cause);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

}
