package com.illtamer.infinite.bot.expansion.mvg.client;

/**
 * mvg 接口调用异常
 * <p>
 * 统一承载 HTTP 状态码、mvg 错误码与错误信息，便于上层把错误翻译成玩家可读文案
 * */
public class MvgApiException extends Exception {

    /**
     * HTTP 状态码，网络异常时为 -1
     * */
    private final int status;

    /**
     * mvg 错误码（如 resources_not_ready / render_failed），网络异常时为空串
     * */
    private final String code;

    public MvgApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code == null ? "" : code;
    }

    public MvgApiException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code == null ? "" : code;
    }

    public static MvgApiException network(String message, Throwable cause) {
        return new MvgApiException(-1, "", message, cause);
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public boolean isNetworkError() {
        return status < 0;
    }

    @Override
    public String toString() {
        return "MvgApiException{status=" + status + ", code='" + code + "', message='" + getMessage() + "'}";
    }

}
