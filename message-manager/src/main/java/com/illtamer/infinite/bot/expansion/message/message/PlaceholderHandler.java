package com.illtamer.infinite.bot.expansion.message.message;

import com.illtamer.infinite.bot.api.Pair;
import com.illtamer.infinite.bot.api.event.message.MessageEvent;
import com.illtamer.infinite.bot.expansion.message.hook.Placeholder;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageEntity;
import com.illtamer.infinite.bot.expansion.message.pojo.MessageNode;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlaceholderHandler {

    private static final Pattern SPLIT_REGX = Pattern.compile("(.*)(\\{)(#[a-zA-Z]+)(})(.*)");
    private final OfflinePlayer player;
    private final Map<String, String> placeholders;

    public PlaceholderHandler(MessageEvent event, OfflinePlayer player) {
        this.player = player;
        this.placeholders = initStaticPlaceholders(event);
    }

    public MessageEntity replace(Pair<MessageNode, Object> pair) {
        MessageEntity entity = new MessageEntity();
        entity.setShowType(pair.getKey().getShowType());
        entity.setAttribute(pair.getValue());
        entity.setContent(doReplace(pair.getKey().getContent()));
        return entity;
    }

    private List<String> doReplace(List<String> content) {
        // splits
        return splitText(
                Placeholder.set(content, player).stream()
                        .map(s -> format(s, placeholders))
                        .collect(Collectors.toList())
        );
    }

    /**
     * 拆分特殊变量的 Key
     * */
    private static List<String> splitText(List<String> list) {
        if (list.size() == 0) return list;
        List<String> splits = new ArrayList<>((int) (list.size() * 1.5));
        for (String line : list) {
            splits.addAll(recursionSplit(line, false));
        }
        splits.remove(splits.size()-1);
        return splits;
    }

    @SuppressWarnings("unchecked")
    private static List<String> recursionSplit(String line, boolean deep) {
        final Matcher matcher = SPLIT_REGX.matcher(line);
        if (!matcher.find()) {
            return deep ? Collections.singletonList(line) :
                    (line.length() == 0 ? Collections.EMPTY_LIST : Arrays.asList(line, "\n"));
        }
        List<String> words = new ArrayList<>();
        final String pre = matcher.group(1);
        if (pre.length() != 0) words.addAll(recursionSplit(pre, true));
        words.add(matcher.group(3));
        final String lat = matcher.group(5);
        if (lat.length() != 0) words.add(lat);
        if (!deep) words.add("\n");
        return words;
    }

    /**
     * 初始化静态变量与内置通用变量
     * */
    private static Map<String, String> initStaticPlaceholders(MessageEvent event) {
        final Map<String, String> placeholders = new HashMap<>(MessageLoader.CUSTOM_PLACEHOLDERS);
        String senderName = event.getSender().getNickname();
        senderName = senderName == null || senderName.length() == 0 ? String.valueOf(event.getSender().getUserId()) : senderName;
        placeholders.put("senderName", senderName);
        return placeholders;
    }

    /**
     * String Format
     * @param format some-{placeholder}-thing
     * @param placeholders placeholder: value
     * @return some-value-thing
     * */
    public static String format(String format, Map<String, String> placeholders) {
        if (format == null || format.length() == 0) return format;
        char[] chars = format.toCharArray();
        final int length = chars.length;
        StringBuilder builder = new StringBuilder(length);

        boolean release = false;
        for (int i = 0; i < length; ++ i) {
            final char c = chars[i];
            if (release) {
                builder.append(c);
                continue;
            }
            // single check
            if (c == '{' && i + 2 < length) {
                boolean replace = false;
                boolean empty = false;
                for (int j = i; j < length; ++j) {
                    if (chars[j] != '}') continue;
                    // do replace. future to support recursion ?
                    String placeholder = placeholders.get(format.substring(i + 1, j));
                    if (!(empty = placeholder == null)) {
                        builder.append(placeholder);
                        i = j; // next start
                        replace = true;
                    }
                    break;
                }
                if (!empty) {
                    // release all chars
                    if (!replace) release = true;
                    else continue;
                }
            }
            builder.append(c);
        }

        return builder.toString();
    }

}
