package com.nahid.userservice.controller;

import com.nahid.userservice.dto.response.ApiResponse;
import com.nahid.userservice.dto.response.UserResponse;
import com.nahid.userservice.dto.response.UserPublicResponse;
import com.nahid.userservice.service.UserService;
import com.nahid.userservice.util.helper.ApiResponseUtil;
import com.nahid.userservice.util.constant.ApiResponseConstant;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse userResponse = userService.getMe();
        return ApiResponseUtil.success(userResponse, ApiResponseConstant.USER_PROFILE_FETCHED);
    }

    // Public endpoint for inter-service communication
    @GetMapping("/public/{userId}")
    public ResponseEntity<ApiResponse<UserPublicResponse>> getUserById(@PathVariable Long userId) {
        UserPublicResponse userResponse = userService.getUserPublicById(userId);
        return ApiResponseUtil.success(userResponse, ApiResponseConstant.USER_PROFILE_FETCHED);
    }
}