package com.illtamer.infinite.bot.expansion.view.exception;

public class ExecuteException extends RuntimeException {

    public ExecuteException() {
    }

    public ExecuteException(Throwable cause) {
        super(cause);
    }

    public ExecuteException(String message) {
        super(message);
    }

    public ExecuteException(String message, Throwable cause) {
        super(message, cause);
    }

}
