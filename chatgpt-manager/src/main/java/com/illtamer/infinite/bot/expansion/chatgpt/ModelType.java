package com.illtamer.infinite.bot.expansion.chatgpt;

public enum ModelType {

    TEXT_DAVINCI_002_RENDER("text-davinci-002-render"),

    TEXT_DAVINCI_003("text-davinci-003");

    private final String name;

    ModelType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ModelType parse(String name) {
        for (ModelType value : ModelType.values()) {
            if (value.name.equals(name)) return value;
        }
        throw new IllegalArgumentException("Unknown model: " + name);
    }

    @Override
    public String toString() {
        return name;
    }

}
