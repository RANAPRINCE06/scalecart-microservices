package com.nahid.notification.controller;

import com.nahid.notification.dto.NotificationDto;
import com.nahid.notification.dto.NotificationResponseDto;
import com.nahid.notification.enums.NotificationStatus;
import com.nahid.notification.service.NotificationService;
import com.nahid.notification.util.constant.ApiResponseConstant;
import com.nahid.notification.util.constant.AppConstant;
import com.nahid.notification.dto.response.ApiResponse;
import com.nahid.notification.util.helper.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationDto>> getNotificationById(@PathVariable UUID id) {
        NotificationDto notification = notificationService.getNotificationById(id);
        return ApiResponseUtil.success(
                notification,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.NOTIFICATION)
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotificationsByUserId(
            @PathVariable String userId) {
        List<NotificationResponseDto> notifications = notificationService.getNotificationsByUserId(userId);
        return ApiResponseUtil.success(
                notifications,
                String.format(ApiResponseConstant.FETCH_ALL_SUCCESSFUL, AppConstant.NOTIFICATIONS)
        );
    }

    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<ApiResponse<Page<NotificationResponseDto>>> getNotificationsByUserIdPaginated(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponseDto> notifications = notificationService.getNotificationsByUserId(userId, pageable);
        return ApiResponseUtil.success(
                notifications,
                String.format(ApiResponseConstant.FETCH_ALL_SUCCESSFUL, AppConstant.NOTIFICATIONS)
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<NotificationDto>> updateNotificationStatus(
            @PathVariable UUID id,
            @RequestParam NotificationStatus status) {
        NotificationDto updatedNotification = notificationService.updateNotificationStatus(id, status);
        return ApiResponseUtil.success(
                updatedNotification,
                String.format(ApiResponseConstant.STATUS_UPDATE_SUCCESSFUL, AppConstant.NOTIFICATION, status.name())
        );
    }

    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getFailedNotifications() {
        List<NotificationResponseDto> failedNotifications = notificationService.getFailedNotifications();
        return ApiResponseUtil.success(
                failedNotifications,
                String.format(ApiResponseConstant.FETCH_ALL_SUCCESSFUL, AppConstant.FAILED_NOTIFICATIONS)
        );
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<ApiResponse<Void>> retryFailedNotifications() {
        notificationService.retryFailedNotifications();
        return ApiResponseUtil.success(
                null,
                String.format(ApiResponseConstant.PROCESS_INITIATED, AppConstant.RETRY_FAILED_NOTIFICATIONS),
                HttpStatus.ACCEPTED
        );
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ApiResponseUtil.success(
                "Notification service is healthy",
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.HEALTH)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationDto>> createNotification(@RequestBody NotificationDto notificationDto) {
        NotificationDto createdNotification = notificationService.createNotification(notificationDto);
        return ApiResponseUtil.success(
                createdNotification,
                String.format(ApiResponseConstant.CREATE_SUCCESSFUL, AppConstant.NOTIFICATION),
                HttpStatus.CREATED
        );
    }
}
