package com.illtamer.infinite.bot.expansion.parse.video.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.illtamer.infinite.bot.expansion.parse.video.model.BiliVideoInfo;
import com.illtamer.infinite.bot.expansion.parse.video.model.BiliVideoUrl;
import com.illtamer.infinite.bot.expansion.parse.video.util.APIUtil;
import com.illtamer.infinite.bot.expansion.parse.video.util.CommonUtil;
import com.illtamer.perpetua.sdk.message.Message;
import com.illtamer.perpetua.sdk.message.MessageBuilder;
import com.illtamer.perpetua.sdk.util.Assert;

import java.io.InterruptedIOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiliVideo {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Pattern BV_PATTERN = Pattern.compile("BV\\w+");
    private static final String VIEW_API = "https://api.bilibili.com/x/web-interface/view?bvid=%s";
    private static final String DOWNLOAD_API = "https://bili.zhouql.vip/download/%d/%d";

    public static Message getBiliURL(String link) throws InterruptedIOException {
        String bvid = parseBiliLink(link);
        BiliVideoInfo info = getVideoInfo(bvid);
        BiliVideoInfo.Stat stat = info.getStat();
        BiliVideoUrl url = getVideoUrl(info.getAvid(), info.getCid());
        MessageBuilder builder = MessageBuilder.json()
                .image(info.getPic(), null)
                .text("标题:" + info.getTitle())
                .text("类型:" + info.getTname() + " | UP:" + info.getOwner().getName() + " | 日期:" + DATE_FORMAT.format(new Date(info.getPubdate() * 1000L)))
                .text("播放:" + CommonUtil.countNum(stat.getView()) + " | 弹幕:" + CommonUtil.countNum(stat.getDanmaku()) + "| 收藏：" + CommonUtil.countNum(stat.getFavorite()))
                .text("点赞:" + CommonUtil.countNum(stat.getLike()) + " | 硬币:" + CommonUtil.countNum(stat.getCoin()) + "| 评论：" + CommonUtil.countNum(stat.getReply()))
                .text("下载链接:");
        List<BiliVideoUrl.SupportFormats> urlSupportFormats = url.getSupportFormats();
        List<BiliVideoUrl.Durl> urlDurls = url.getDurls();
        for (int i = 0; i < urlDurls.size(); ++ i) {
            BiliVideoUrl.Durl durl = urlDurls.get(i);
            BiliVideoUrl.SupportFormats supportFormats = urlSupportFormats.get(i);
            builder.text(String.format("%d. [%s] (%s) %s", i+1, supportFormats.getDisplayDesc(), CommonUtil.countSize(durl.getSize()), durl.getUrl()));
        }
        return builder.build();
    }

    // @return bvid
    public static String parseBiliLink(String link) throws InterruptedIOException {
        if (link.contains("BV")) {
            int begin = link.indexOf("BV");
            int end = link.indexOf("/", begin);
            end = end == -1 ? link.length() : end;
            return link.substring(begin, end);
        }

        String resp = APIUtil.getBody(link, false);
        Assert.notNull(resp, "Can't parse bili link");
        Matcher matcher = BV_PATTERN.matcher(resp);

        Assert.isTrue(matcher.find(), "Can't match content: " + resp);
        return matcher.group(0);
    }

    public static BiliVideoInfo getVideoInfo(String bvid) throws InterruptedIOException {
        String url = String.format(VIEW_API, bvid);
        return invokeAPI(url, BiliVideoInfo.class);
    }

    public static BiliVideoUrl getVideoUrl(Long avid, Long cid) throws InterruptedIOException {
        String url = String.format(DOWNLOAD_API, avid, cid);
        return invokeAPI(url, BiliVideoUrl.class);
    }

    private static <T> T invokeAPI(String url, Class<T> clazz) throws InterruptedIOException {
        String json = APIUtil.getBody(url, true);
        Gson gson = new Gson();
        JsonObject response = gson.fromJson(json, JsonObject.class);
        int code = response.get("code").getAsInt();
        Assert.isTrue(code == 0, "Error response code: " + code);
        return gson.fromJson(response.getAsJsonObject("data"), clazz);
    }

}
