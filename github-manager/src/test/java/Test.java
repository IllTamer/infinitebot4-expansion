import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class Test {
    public static void main(String[] args) throws UnsupportedEncodingException {
        //鏇存柊鐜╁鏁版嵁鏃堕儴鍒嗗瓧娈典涪澶辩殑闂
        String test= "鏇存柊鐜╁\uE18D鏁版嵁鏃堕儴鍒嗗瓧娈典涪澶辩殑闂\uE1C0\uE57D";
        System.out.println(test);
        System.out.println(new String(test.getBytes()));
        System.out.println(new String(test.getBytes(), "GB2312"));
        System.out.println(new String(test.getBytes(), "ISO8859_1"));
        System.out.println(new String(test.getBytes("GBK"), StandardCharsets.UTF_8));
    }

}
