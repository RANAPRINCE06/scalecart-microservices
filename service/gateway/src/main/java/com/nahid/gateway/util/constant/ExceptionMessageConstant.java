package com.nahid.gateway.util.constant;

public class ExceptionMessageConstant {

    public static final String JWT_TOKEN_EXPIRED = "JWT token expired";
    public static final String JWT_TOKEN_INVALID = "JWT token invalid";
    public static final String JWT_TOKEN_MISSING = "JWT token missing";
    public static final String JWT_TOKEN_UNAUTHORIZED = "JWT token unauthorized";
    private ExceptionMessageConstant() {
        throw new UnsupportedOperationException("Utility class");
    }
}
