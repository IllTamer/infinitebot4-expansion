package com.illtamer.infinite.bot.expansion.message.message;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.expansion.message.pojo.Limit;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageNode;
import com.illtamer.infinite.bot.minecraft.expansion.Language;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class MessageLimit {

    private static final Map<Integer, Pair<Date, Integer>> CACHED_MAX_PER_MINUTE = new HashMap<>();
    private static final Map<Integer, Map<Long, Date>> CACHED_USER_TRIGGER_INTERVAL = new HashMap<>();

    private static boolean noticeWhenLimit;
    private static Language language;

    public static void init(FileConfiguration config, Language _language) {
        noticeWhenLimit = config.getBoolean("notice-when-limit");
        language = _language;
    }

    public static List<Pair<MessageNode, Object>> check(List<Pair<MessageNode, Object>> list, MessageEvent event) {
        final int oriCount = list.size();
        final long userId = event.getUserId();
        final Iterator<Pair<MessageNode, Object>> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Pair<MessageNode, Object> pair = iterator.next();
            final Limit limit = pair.getKey().getLimit();
            final int hashCode = pair.getKey().hashCode();
            final int maxPerMinute = limit.getMaxPerMinute();
            if (maxPerMinute != 0 && !checkMaxPerMinute(hashCode, maxPerMinute)) {
                iterator.remove();
                continue;
            }
            final int userTriggerInterval = limit.getUserTriggerInterval();
            if (userTriggerInterval != 0 && !checkUserTriggerInterval(hashCode, userId, userTriggerInterval)) {
                iterator.remove();
            }
        }

        if (noticeWhenLimit && oriCount != list.size()) {
            event.reply(language.get("limit").replace("%count%", String.valueOf(oriCount - list.size())));
        }
        return list;
    }

    private static boolean checkMaxPerMinute(int hash, int maxPerMinute) {
        final Pair<Date, Integer> pair = CACHED_MAX_PER_MINUTE.get(hash);
        if (pair == null || isOutTime(pair.getKey(), 60)) {
            CACHED_MAX_PER_MINUTE.put(hash, new Pair<>(new Date(), 1));
            return true;
        }
        final Integer value = pair.getValue();
        if (value < maxPerMinute) {
            pair.setValue(value+1);
            return true;
        }
        return false;
    }

    private static boolean checkUserTriggerInterval(int hash, long userId, int userTriggerInterval) {
        final Map<Long, Date> dateMap = CACHED_USER_TRIGGER_INTERVAL.computeIfAbsent(hash, key -> new HashMap<>());
        final Date date = dateMap.get(userId);
        if (date == null || isOutTime(date, userTriggerInterval)) {
            dateMap.put(userId, new Date());
            return true;
        }
        return false;
    }

    // 超出记录分钟
    private static boolean isOutTime(Date time, int seconds) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        calendar.add(Calendar.SECOND, seconds);
        return !calendar.getTime().after(new Date());
    }

}
