package com.nahid.notification.service;

import com.nahid.notification.dto.NotificationDto;
import com.nahid.notification.dto.NotificationResponseDto;
import com.nahid.notification.dto.OrderEventDto;
import com.nahid.notification.dto.PaymentNotificationDto;
import com.nahid.notification.entity.Notification;
import com.nahid.notification.enums.NotificationStatus;
import com.nahid.notification.enums.ReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationDto createNotification(NotificationDto notificationDto);

    NotificationDto getNotificationById(UUID id);

    List<NotificationResponseDto> getNotificationsByUserId(String userId);

    Page<NotificationResponseDto> getNotificationsByUserId(String userId, Pageable pageable);

    NotificationDto updateNotificationStatus(UUID id, NotificationStatus status);

    void processPaymentNotification(PaymentNotificationDto paymentNotificationDto);

    void processOrderNotification(OrderEventDto orderEventDto);

    void retryFailedNotifications();

    List<NotificationResponseDto> getFailedNotifications();

    void sendSmsNotification(Notification notification);

    void sendEmailNotification(Notification notification);

    boolean isDuplicateNotification(UUID referenceId, ReferenceType referenceType);
}