package com.nahid.product.exception;

public class StockUpdateException extends RuntimeException {

    public StockUpdateException(String message) {
        super(message);
    }

    public StockUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}

