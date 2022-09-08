package com.illtamer.infinite.bot.expansion.message.pojo;

import java.util.List;

public class MessageEntity {

    private MessageNode.ShowType showType;

    /**
     * Replaced clean messages
     * */
    private List<String> content;

    /**
     * 特定 ShowType 所具有的的属性实例
     * */
    private Object attribute;

    public MessageNode.ShowType getShowType() {
        return showType;
    }

    public void setShowType(MessageNode.ShowType showType) {
        this.showType = showType;
    }

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public Object getAttribute() {
        return attribute;
    }

    public void setAttribute(Object attribute) {
        this.attribute = attribute;
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "showType=" + showType +
                ", content=" + content +
                ", attribute=" + attribute +
                '}';
    }

}
