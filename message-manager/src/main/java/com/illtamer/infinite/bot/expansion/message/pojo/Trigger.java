package com.illtamer.infinite.bot.expansion.message.pojo;

import java.util.List;

/**
 * 触发器
 * */
public class Trigger {

    /**
     * 允许触发的消息来源
     * */
    private Source source;

    /**
     * 仅允许bot管理员触发
     * */
    private Boolean admin;

    /**
     * 是否仅允许已绑定玩家触发
     * */
    private Boolean bind;

    /**
     * 触发类型
     * */
    private Type type;

    /**
     * 过滤器选项
     * */
    private Boolean filter;

    /**
     * 关键字列表
     * */
    private List<String> keys;

    /**
     * 允许触发的消息来源
     * */
    public enum Source {

        /**
         * 仅群消息可触发
         * */
        GROUP("group"),

        /**
         * 仅私聊可触发
         * */
        PRIVATE("private"),

        /**
         * 不限制触发方式
         * */
        ALL("all");

        private final String value;

        Source(String value) {
            this.value = value;
        }

        public static Source parse(String value) {
            if (value != null) {
                if (GROUP.value.equals(value)) return GROUP;
                else if (PRIVATE.value.equals(value)) return PRIVATE;
                else if (ALL.value.equals(value)) return ALL;
            }
            throw new IllegalArgumentException("Unknown source: " + value);
        }

        public String getValue() {
            return value;
        }

    }

    /**
     * 触发类型
     * */
    public enum Type {

        CONTAINS("contains"),

        EQUAL("equal");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Type parse(String value) {
            if (value != null) {
                if (CONTAINS.value.equals(value)) return CONTAINS;
                else if (EQUAL.value.equals(value)) return EQUAL;
            }
            throw new IllegalArgumentException("Unknown type: " + value);
        }

    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Boolean getBind() {
        return bind;
    }

    public void setBind(Boolean bind) {
        this.bind = bind;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Boolean getFilter() {
        return filter;
    }

    public void setFilter(Boolean filter) {
        this.filter = filter;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    @Override
    public String toString() {
        return "Trigger{" +
                "source=" + source +
                ", admin=" + admin +
                ", bind=" + bind +
                ", type=" + type +
                ", filter=" + filter +
                ", keys=" + keys +
                '}';
    }

}
