package com.nahid.order.client;

import com.nahid.order.dto.response.ApiResponse;
import com.nahid.order.dto.response.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        fallback = UserFeignClientFallback.class)
public interface UserClient {

    @GetMapping("/api/users/public/{userId}")
    ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable("userId") Long userId);
}
