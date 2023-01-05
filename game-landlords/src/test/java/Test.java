import com.illtamer.infinite.bot.api.config.CQHttpWebSocketConfiguration;
import com.illtamer.infinite.bot.api.handler.OpenAPIHandling;
import com.illtamer.infinite.bot.api.message.Message;
import com.illtamer.infinite.bot.api.message.MessageBuilder;
import com.illtamer.infinite.bot.expansion.landlords.core.GameCenter;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.Card;
import com.illtamer.infinite.bot.expansion.landlords.core.pojo.Participant;
import com.illtamer.infinite.bot.expansion.landlords.graphic.Brush;
import com.illtamer.infinite.bot.expansion.landlords.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Test {

    // A[♥](0), 2[♠](1), 3[♦](2), 4[♣](3), 5[♥](4), 6[♠](5), 7[♦](6), 8[♣](7), 9[♥](8), 10[♠](9), J[♦](10), Q[♣](11), K[♥](12),
    // A[♠](13), 2[♦](14), 3[♣](15), 4[♥](16), 5[♠](17), 6[♦](18), 7[♣](19), 8[♥](20), 9[♠](21), 10[♦](22), J[♣](23), Q[♥](24), K[♠](25),
    // A[♦](26), 2[♣](27), 3[♥](28), 4[♠](29), 5[♦](30), 6[♣](31), 7[♥](32), 8[♠](33), 9[♦](34), 10[♣](35), J[♥](36), Q[♠](37), K[♦](38),
    // A[♣](39), 2[♥](40), 3[♠](41), 4[♦](42), 5[♣](43), 6[♥](44), 7[♠](45), 8[♦](46), 9[♣](47), 10[♥](48), J[♠](49), Q[♦](50), K[♣](51),
    // 小王(52), 大王(53)
    public static void main(String[] args) throws Exception {
        CQHttpWebSocketConfiguration.setHttpUri("http://47.117.136.149:5700");
        CQHttpWebSocketConfiguration.setAuthorization("root765743073");

//        Participant p1 = new Participant();
//        Participant p2 = new Participant();
//        Participant p3 = new Participant();
//        GameCenter.startup(Arrays.asList(p1, p2, p3));

//        final BufferedImage image = Brush.drawHandCardList(p1.getCardList());
//        final BufferedImage remainImage = Brush.drawTotalCardList(remain);
//        ImageIO.write(image, "png", new File("C:\\Users\\Bacon\\Desktop\\0.png"));

//        p1.getCardList().sort(Collections.reverseOrder());
//        System.out.println(p1.getCardList());
//        System.out.println(GameCenter.canPlayHandCard(p1, "3"));

//        final Message message = MessageBuilder.json()
//                .at(765743073L)
//                .text("地主牌为")
//                .image("0.png", ImageUtil.imageToBase64(remainImage))
//                .text("您的手牌如下")
//                .image("text.png", ImageUtil.imageToBase64(image)).build();
//        OpenAPIHandling.sendGroupMessage(message, 863522624);
    }

//    public static void main(String[] args) {
//
//
//    }

}
