package com.illtamer.infinite.bot.expansion.manager.basic.util;

import com.illtamer.infinite.bot.minecraft.start.bukkit.BukkitBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Bukkit 调度器兼容工具类（兼容核心 4.1.0，无需 MinecraftScheduler）
 */
public final class SchedulerCompat {

    private SchedulerCompat() {}

    private static Plugin plugin() {
        return BukkitBootstrap.getInstance();
    }

    /** 主线程执行 */
    public static BukkitTask runTask(Runnable runnable) {
        return Bukkit.getScheduler().runTask(plugin(), runnable);
    }

    /** 异步执行 */
    public static BukkitTask runTaskAsync(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin(), runnable);
    }

    /** 异步延迟执行 */
    public static BukkitTask runTaskLaterAsync(Runnable runnable, long delay, TimeUnit unit) {
        long ticks = Math.max(1L, unit.toMillis(delay) / 50L);
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin(), runnable, ticks);
    }

    /** 主线程延迟执行 */
    public static BukkitTask runTaskLater(Runnable runnable, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin(), runnable, delayTicks);
    }

    /** 同步调用并返回结果（需在非主线程调用） */
    public static <T> T callSyncGlobal(Callable<T> callable) {
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin(), callable).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 实体相关同步调用（Paper 下等价于主线程） */
    public static <T> T callSyncEntity(Entity entity, Callable<T> callable) {
        return callSyncGlobal(callable);
    }

    /** 实体延迟任务；返回 null 表示实体已离线 */
    public static BukkitTask runEntityTaskLater(Entity entity, Runnable runnable, Runnable retired, long delayTicks) {
        if (entity == null || !entity.isValid()) {
            return null;
        }
        return Bukkit.getScheduler().runTaskLater(plugin(), runnable, delayTicks);
    }

}
