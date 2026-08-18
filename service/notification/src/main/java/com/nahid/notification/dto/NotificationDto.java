package com.nahid.notification.dto;

import com.nahid.notification.entity.Notification;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.nahid.notification.enums.NotificationStatus;
import com.nahid.notification.enums.NotificationType;
import com.nahid.notification.enums.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID id;
    private UUID referenceId;
    private ReferenceType referenceType;
    private String userId;
    private String userEmail;
    private String userPhone;
    private NotificationType notificationType;
    private String message;
    private NotificationStatus status;
    private BigDecimal amount;
    private String currency;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    private String errorMessage;
    private Integer retryCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
