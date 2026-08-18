package com.nahid.payment.exception;

import java.io.Serial;

public class PaymentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentException(Throwable cause) {
        super(cause);
    }
}
