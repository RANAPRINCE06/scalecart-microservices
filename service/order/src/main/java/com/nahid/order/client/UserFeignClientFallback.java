package com.nahid.order.client;

import com.nahid.order.dto.response.ApiResponse;
import com.nahid.order.dto.response.UserResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class UserFeignClientFallback implements UserClient {

    @Override
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(Long userId) {
        log.warn("Fallback executed for downstream service [user-service], operation [getUserById] for userId: {}", userId);
        ApiResponse<UserResponseDto> response = ApiResponse.<UserResponseDto>builder()
                .statusCode(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE.value())
                .success(false)
                .message("User service is unavailable. Unable to validate user ID: " + userId)
                .data(null)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}