package com.illtamer.infinite.bot.expansion.parse.video.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BiliVideoUrl {

    /**
     * 视频格式
     * */
    private String format;

    /**
     * 视频链接列表
     * */
    @SerializedName("durl")
    private List<Durl> durls;

    /**
     * 视频支持格式列表
     * */
    @SerializedName("support_formats")
    private List<SupportFormats> supportFormats;

    public static final class Durl {

        /**
         * 字节数
         * */
        private Long size;

        private String url;

        public Long getSize() {
            return size;
        }

        public String getUrl() {
            return url;
        }

    }

    public static final class SupportFormats {

        /**
         * 格式
         * */
        private String format;

        /**
         * 清晰度
         * */
        @SerializedName("display_desc")
        private String displayDesc;

        public String getFormat() {
            return format;
        }

        public String getDisplayDesc() {
            return displayDesc;
        }

    }

    public String getFormat() {
        return format;
    }

    public List<Durl> getDurls() {
        return durls;
    }

    public List<SupportFormats> getSupportFormats() {
        return supportFormats;
    }

}
