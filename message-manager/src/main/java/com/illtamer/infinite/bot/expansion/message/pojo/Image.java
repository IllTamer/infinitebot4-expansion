package com.illtamer.infinite.bot.expansion.message.pojo;

import java.awt.*;

/**
 * 图片消息配置节点
 * */
public class Image {

    /**
     * 图片资源路径
     * */
    private String source;

    /**
     * 图片的宽
     * */
    private Integer width;

    /**
     * 图片的高
     * */
    private Integer height;

    /**
     * 文本颜色
     * */
    private Color color;

    /**
     * 文本尺寸
     * */
    private Integer size;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

}
