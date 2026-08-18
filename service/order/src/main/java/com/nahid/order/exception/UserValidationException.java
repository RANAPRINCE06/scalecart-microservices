package com.nahid.order.exception;

public class UserValidationException extends RuntimeException {
    public UserValidationException(String message) {
        super(message);
    }
    public UserValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
