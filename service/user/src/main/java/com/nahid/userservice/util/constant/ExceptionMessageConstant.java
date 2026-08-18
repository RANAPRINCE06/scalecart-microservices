package com.nahid.userservice.util.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExceptionMessageConstant {

    public static final String ENTITY_NOT_FOUND_BY_ID = "%s not found with id: %s";
    public static final String ENTITY_NOT_FOUND_BY_FIELD = "%s not found with %s: %s";
    public static final String ENTITY_ALREADY_EXISTS = "%s already exists with %s: %s";
    public static final String UNIQUE_FIELD_VIOLATION = "%s must be unique";
    public static final String ERROR_OCCURRED = "%s error occurred due to %s";
    public static final String ILLEGAL_OBJECT = "Illegal object: %s";
    public static final String INVALID_REQUEST = "Invalid request: %s";
    public static final String INVALID_REQUEST_TEMPLATE = "Invalid request: %s. %s";
    public static final String STATUS_CHANGE_ERROR = "Cannot change %s status from %s to %s";

    // Authentication-related constants
    public static final String EMAIL_ALREADY_REGISTERED = "Email already registered";
    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String REFRESH_TOKEN_REVOKED = "Refresh token has been revoked";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
    public static final String REFRESH_TOKEN_ALREADY_REVOKED = "Refresh token has already been revoked";
    public static final String REFRESH_TOKEN_OWNERSHIP_MISMATCH = "Refresh token does not belong to authenticated user";
    public static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred";

}
