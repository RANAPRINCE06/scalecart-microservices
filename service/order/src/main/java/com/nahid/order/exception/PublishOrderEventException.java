package com.nahid.order.exception;

public class PublishOrderEventException extends RuntimeException {
    public PublishOrderEventException(String message) {
        super(message);
    }

    public PublishOrderEventException(String message, Throwable cause) {
        super(message, cause);
    }


}
