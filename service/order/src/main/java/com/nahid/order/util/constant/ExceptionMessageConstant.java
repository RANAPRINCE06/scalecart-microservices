package com.nahid.order.util.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExceptionMessageConstant {

    // Generic Exception Messages
    public static final String ENTITY_NOT_FOUND_BY_ID = "%s not found with id: %s";
    public static final String ENTITY_NOT_FOUND_BY_FIELD = "%s not found with %s: %s";
    public static final String INVALID_REQUEST = "Invalid request: %s";
    public static final String VALIDATION_FAILED = "Validation failed: %s";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred: %s";

    // Order-specific Exception Messages
    public static final String ORDER_NOT_FOUND = "Order not found with ID: %s";
    public static final String ORDER_NOT_FOUND_BY_NUMBER = "Order not found with order number: %s";
    public static final String ORDER_PROCESSING_FAILED = "Order processing failed: %s";
    public static final String ORDER_CREATION_FAILED = "Failed to create order: %s";
    public static final String ORDER_UPDATE_FAILED = "Failed to update order: %s";
    public static final String ORDER_CANCELLATION_FAILED = "Failed to cancel order: %s";

    // Order Status Exception Messages
    public static final String INVALID_STATUS_TRANSITION = "Cannot change order status from %s to %s";
    public static final String ORDER_CANNOT_BE_CANCELLED = "Order cannot be cancelled in current status: %s";

    // User Validation Exception Messages
    public static final String USER_NOT_FOUND = "User not found with ID: %s";
    public static final String USER_SUSPENDED = "User is suspended";
    public static final String USER_INACTIVE = "User is inactive";
    public static final String USER_BLOCKED = "User is blocked";
    public static final String USER_VALIDATION_FAILED = "User validation failed for ID: %s";

    // Product-related Exception Messages
    public static final String PRODUCT_RESERVATION_FAILED = "Product reservation failed: %s";
    public static final String PRODUCT_PRICE_FETCH_FAILED = "Product price lookup failed: %s";


    // Event Exception Messages
    public static final String EVENT_PUBLISH_FAILED = "Failed to publish event: %s";
    public static final String EVENT_PUBLISH_SUCCESSFUL = "Event published successfully: %s";
}
