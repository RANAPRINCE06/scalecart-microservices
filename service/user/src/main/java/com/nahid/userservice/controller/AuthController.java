package com.nahid.userservice.controller;

import com.nahid.userservice.dto.request.AuthRequest;
import com.nahid.userservice.dto.request.RegisterRequest;
import com.nahid.userservice.dto.response.ApiResponse;
import com.nahid.userservice.dto.response.AuthResponse;
import com.nahid.userservice.dto.response.LogoutResponse;
import com.nahid.userservice.dto.response.RegisterResponse;
import com.nahid.userservice.service.AuthService;
import com.nahid.userservice.service.UserService;
import com.nahid.userservice.util.helper.ApiResponseUtil;
import com.nahid.userservice.util.constant.ApiResponseConstant;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponseUtil.success(authService.register(request), ApiResponseConstant.USER_REGISTERED_SUCCESSFULLY, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ApiResponseUtil.success(authService.login(request), ApiResponseConstant.LOGIN_SUCCESSFUL);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        return ApiResponseUtil.success(authService.refreshToken(authHeader), ApiResponseConstant.TOKEN_REFRESHED_SUCCESSFULLY);
    }

    @PostMapping("/logout")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(@RequestHeader(value = "Authorization", required = false)String authHeader) {
        return ApiResponseUtil.success(null, userService.logout(authHeader));
    }
}