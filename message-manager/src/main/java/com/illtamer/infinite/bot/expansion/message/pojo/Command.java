package com.illtamer.infinite.bot.expansion.message.pojo;

public class Command {

    /**
     * 是否以op权限执行
     * */
    private boolean op;

    /**
     * 执行范围
     * 'console': 执行到控制台
     * 'self': 执行到每个玩家
     * */
    private Type type;

    public enum Type {

        CONSOLE("console"),

        SELF("self");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type parse(String value) {
            if (CONSOLE.value.equals(value)) return CONSOLE;
            else if (SELF.value.equals(value)) return SELF;
            throw new IllegalArgumentException("Unknown type: " + value);
        }

    }

    public boolean isOp() {
        return op;
    }

    public void setOp(boolean op) {
        this.op = op;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

}
