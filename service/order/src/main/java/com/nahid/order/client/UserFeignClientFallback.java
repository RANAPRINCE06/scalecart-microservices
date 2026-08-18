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
        ApiResponse<UserResponseDto> response = ApiResponse.<UserResponseDto>builder()
                .statusCode(503)
                .success(false)
                .message("User service is unavailable")
                .data(null)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.ok(response);
    }

}