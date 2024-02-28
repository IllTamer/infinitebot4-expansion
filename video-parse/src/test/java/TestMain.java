import com.illtamer.infinite.bot.expansion.parse.video.api.BiliVideo;
import com.illtamer.infinite.bot.expansion.parse.video.model.BiliVideoInfo;
import com.illtamer.infinite.bot.expansion.parse.video.model.BiliVideoUrl;
import com.illtamer.infinite.bot.minecraft.api.BotScheduler;
import com.illtamer.perpetua.sdk.message.Message;

import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.Executors;

public class TestMain {

    // [{"type":"image","data":{"url":"http://i2.hdslb.com/bfs/archive/ca4c2b575fdc1456b6975c09ea4e95689d52e18a.jpg","file":"cover.jpg"}},{"type":"text","data":{"text":"\n标题：【CS2-DUST2】炙热沙城2你可能会犯的7个错误"}},{"type":"text","data":{"text":"\n类型：电子竞技| UP：马场电竞CS2| 日期：1970-01-21 02:10:43"}},{"type":"text","data":{"text":"\n播放：8.13| 弹幕：0.02万| 收藏：0.34万"}},{"type":"text","data":{"text":"\n点赞：0.43万| 硬币：0.19万| 评论：0.02万"}},{"type":"text","data":{"text":"\n下载链接："}},{"type":"text","data":{"text":"\n[360P] (45.71MB) https://cn-xj-ct-01-02.bilivideo.com/upgcxcode/76/91/1429979176/1429979176-1-16.mp4?e=ig8euxZM2rNcNbRVhwdVhwdlhWdVhwdVhoNvNC8BqJIzNbfq9rVEuxTEnE8L5F6VnEsSTx0vkX8fqJeYTj_lta53NCM=&uipk=5&nbs=1&deadline=1708319971&gen=playurlv2&os=bcache&oi=2043500563&trid=000050be6276b44d405389460960828dfd27h&mid=0&platform=html5&upsig=54e97530a9c669b9956b4d6c95aa4dfc&uparams=e,uipk,nbs,deadline,gen,os,oi,trid,mid,platform&cdnid=87002&bvc=vod&nettype=0&f=h_0_0&bw=53082&logo=80000000"}}]
    public static void main(String[] args) {
//        String link1 = "https://www.bilibili.com/video/BV1Xx4y1Z7sD/?spm_id_from=333.1007.tianma.1-1-1.click&vd_source=07787e9fcd702835ebff727ff3181efd";
        String link2 = "https://b23.tv/aFSs9TT";
//        String link = BiliVideo.parseBiliLink(link2);
        Executors.newFixedThreadPool(1).submit(() -> {
            try {
                Message message = BiliVideo.getBiliURL(link2);
                System.out.println(message);
            } catch (Exception e) {
                if (e instanceof InterruptedIOException)
                    System.out.println("API 调用超时: " + e.getMessage());
                else
                    e.printStackTrace();
            }
        });
    }

}
