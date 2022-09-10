package com.illtamer.infinite.bot.expansion.message.pojo;

import com.illtamer.infinite.bot.expansion.message.InputStreamSupplier;

import java.awt.*;
import java.util.List;

/**
 * 图片消息配置节点
 * */
public class Image {

    /**
     * 字体设置
     * */
    private Font font;

    /**
     * 图片相关设置
     * */
    private Setting setting;

    /**
     * 插入图片设置
     * */
    private List<InsertImage> insertImages;

    /**
     * 图片相关设置
     * */
    public static class Setting {

        /**
         * 图片资源路径
         * */
        private InputStreamSupplier source;

        /**
         * 图片的宽
         * */
        private Integer width;

        /**
         * 图片的高
         * */
        private Integer height;

        /**
         * 文字起始横坐标
         * */
        private Integer startX;

        /**
         * 文字起始纵坐标
         * */
        private Integer startY;

        /**
         * 文字行间距
         * */
        private Integer space;

        /**
         * 文本颜色
         * */
        private Color color;

        public InputStreamSupplier getSource() {
            return source;
        }

        public void setSource(InputStreamSupplier source) {
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

        public Integer getStartX() {
            return startX;
        }

        public void setStartX(Integer startX) {
            this.startX = startX;
        }

        public Integer getStartY() {
            return startY;
        }

        public void setStartY(Integer startY) {
            this.startY = startY;
        }

        public Integer getSpace() {
            return space;
        }

        public void setSpace(Integer space) {
            this.space = space;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public String toString() {
            return "Setting{" +
                    "source=" + source +
                    ", width=" + width +
                    ", height=" + height +
                    ", startX=" + startX +
                    ", startY=" + startY +
                    ", space=" + space +
                    ", color=" + color +
                    '}';
        }

    }

    /**
     * 插入图片设置
     * */
    public static class InsertImage {

        /**
         * 图片资源路径
         * */
        private InputStreamSupplier source;

        /**
         * 图片的宽
         * */
        private Integer width;

        /**
         * 图片的高
         * */
        private Integer height;

        /**
         * 文字起始横坐标
         * */
        private Integer startX;

        /**
         * 文字起始纵坐标
         * */
        private Integer startY;

        public InputStreamSupplier getSource() {
            return source;
        }

        public void setSource(InputStreamSupplier source) {
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

        public Integer getStartX() {
            return startX;
        }

        public void setStartX(Integer startX) {
            this.startX = startX;
        }

        public Integer getStartY() {
            return startY;
        }

        public void setStartY(Integer startY) {
            this.startY = startY;
        }

        @Override
        public String toString() {
            return "InsertImage{" +
                    "source=" + source +
                    ", width=" + width +
                    ", height=" + height +
                    ", startX=" + startX +
                    ", startY=" + startY +
                    '}';
        }

    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Setting getSetting() {
        return setting;
    }

    public void setSetting(Setting setting) {
        this.setting = setting;
    }

    public List<InsertImage> getInsertImages() {
        return insertImages;
    }

    public void setInsertImages(List<InsertImage> insertImages) {
        this.insertImages = insertImages;
    }

    @Override
    public String toString() {
        return "Image{" +
                "font=" + font +
                ", setting=" + setting +
                ", insertImages=" + insertImages +
                '}';
    }

}
