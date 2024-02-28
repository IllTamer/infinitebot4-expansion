package com.illtamer.infinite.bot.expansion.parse.video.model;

import com.google.gson.annotations.SerializedName;

public class BiliVideoInfo {

    @SerializedName("aid")
    private Long avid;

    private Long cid;

    /**
     * 封面图片
     * */
    private String pic;

    /**
     * 分类
     * */
    private String tname;

    /**
     * 标题
     * */
    private String title;

    /**
     * 简介
     * */
    private String desc;

    /**
     * 作者数据
     * */
    private Owner owner;

    /**
     * 发布时间
     * */
    private Long pubdate;

    /**
     * 视频状态参数
     * */
    private Stat stat;

    public final static class Owner {

        /**
         * 作者名称
         * */
        private String name;

        public String getName() {
            return name;
        }

    }

    public final static class Stat {

        /**
         * 播放量
         * */
        private Integer view;

        /**
         * 点赞数
         * */
        private Integer like;

        /**
         * 弹幕数
         * */
        private Integer danmaku;

        /**
         * 评论数
         * */
        private Integer reply;

        /**
         * 硬币数
         * */
        private Integer coin;

        /**
         * 收藏数
         * */
        private Integer favorite;

        /**
         * 转发数
         * */
        private Integer share;

        public Integer getView() {
            return view;
        }

        public Integer getLike() {
            return like;
        }

        public Integer getDanmaku() {
            return danmaku;
        }

        public Integer getReply() {
            return reply;
        }

        public Integer getCoin() {
            return coin;
        }

        public Integer getFavorite() {
            return favorite;
        }

        public Integer getShare() {
            return share;
        }

    }

    public Long getAvid() {
        return avid;
    }

    public Long getCid() {
        return cid;
    }

    public String getPic() {
        return pic;
    }

    public String getTname() {
        return tname;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public Owner getOwner() {
        return owner;
    }

    public Long getPubdate() {
        return pubdate;
    }

    public Stat getStat() {
        return stat;
    }

}
