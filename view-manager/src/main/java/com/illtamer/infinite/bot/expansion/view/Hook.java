package com.illtamer.infinite.bot.expansion.view;

import com.loohp.interactivechat.InteractiveChat;

import java.util.regex.Pattern;

public class Hook {

    public static final Pattern ITEM_PLACEHOLDER;
    public static final Pattern INV_PLACEHOLDER;
    public static final Pattern END_PLACEHOLDER;

    static  {
        ITEM_PLACEHOLDER = InteractiveChat.itemPlaceholder;
        INV_PLACEHOLDER = InteractiveChat.invPlaceholder;
        END_PLACEHOLDER = InteractiveChat.enderPlaceholder;
    }

    static void init() {}

}
