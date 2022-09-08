package com.illtamer.infinite.bot.expansion.message.pojo;

import java.util.List;

/**
 * 消息节点
 * */
public class MessageNode {

    /**
     * 消息触发器
     * */
    private Trigger trigger;

    /**
     * 是否启用papi变量
     * */
    private Boolean papi;

    /**
     * 文字展现方式
     * */
    private ShowType showType;

    /**
     * 消息内容
     * */
    private List<String> content;

    /**
     * 文字展现方式
     * */
    public enum ShowType {

        /**
         * 文本
         * */
        TEXT("text"),

        /**
         * 图片
         * */
        IMAGE("image");

        private final String value;

        ShowType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ShowType parse(String value) {
            if (value != null) {
                if (value.equals(TEXT.value)) return TEXT;
                else if (value.equals(IMAGE.value)) return IMAGE;
            }
            throw new IllegalArgumentException("Unknown show-type: " + value);
        }

    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Boolean getPapi() {
        return papi;
    }

    public void setPapi(Boolean papi) {
        this.papi = papi;
    }

    public ShowType getShowType() {
        return showType;
    }

    public void setShowType(ShowType showType) {
        this.showType = showType;
    }

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

}
