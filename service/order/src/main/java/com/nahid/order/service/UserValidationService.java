package com.nahid.order.service;

import jakarta.validation.constraints.NotNull;

public interface UserValidationService {

    void validateUserForOrder(@NotNull(message = "User ID is required") Long userId);
}
