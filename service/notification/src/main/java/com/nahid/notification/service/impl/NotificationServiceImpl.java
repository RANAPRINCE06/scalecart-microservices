package com.nahid.notification.service.impl;

import com.nahid.notification.dto.NotificationDto;
import com.nahid.notification.dto.NotificationResponseDto;
import com.nahid.notification.dto.OrderEventDto;
import com.nahid.notification.dto.PaymentNotificationDto;
import com.nahid.notification.entity.Notification;
import com.nahid.notification.enums.NotificationStatus;
import com.nahid.notification.enums.ReferenceType;
import com.nahid.notification.exception.NotificationNotFoundException;
import com.nahid.notification.mapper.NotificationMapper;
import com.nahid.notification.repository.NotificationRepository;
import com.nahid.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public NotificationDto createNotification(NotificationDto notificationDto) {
        Notification notification = notificationMapper.toEntity(notificationDto);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);
        Notification savedNotification = notificationRepository.save(notification);

        return notificationMapper.toDto(savedNotification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto getNotificationById(UUID id) {

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + id));

        return notificationMapper.toDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsByUserId(String customerId) {


        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(customerId);
        return notifications.stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotificationsByUserId(String customerId, Pageable pageable) {

        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(customerId, pageable);
        return notifications.map(notificationMapper::toResponseDto);
    }

    @Override
    public NotificationDto updateNotificationStatus(UUID id, NotificationStatus status) {

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with ID: " + id));

        notification.setStatus(status);
        if (status == NotificationStatus.SENT) {
            notification.setSentAt(LocalDateTime.now());
        }

        Notification updatedNotification = notificationRepository.save(notification);
        return notificationMapper.toDto(updatedNotification);
    }

    @Override
    public void processPaymentNotification(PaymentNotificationDto paymentNotificationDto) {
        try {
            Notification notification = notificationMapper.paymentDtoToEntity(paymentNotificationDto);

            if (notification.getMessage() == null || notification.getMessage().isEmpty()) {
                notification.setMessage(generatePaymentMessage(paymentNotificationDto));
            }
            Notification savedNotification = notificationRepository.save(notification);

            switch (notification.getNotificationType()) {
                case SMS -> sendSmsNotification(savedNotification);
                case EMAIL -> sendEmailNotification(savedNotification);
                default -> log.warn("Unsupported notification type: {}", notification.getNotificationType());
            }

        } catch (Exception e) {

            throw new RuntimeException("Failed to process payment notification", e);
        }
    }

    @Override
    public void processOrderNotification(OrderEventDto orderEventDto) {

        try {
            if (isDuplicateNotification(orderEventDto.getOrderId(), ReferenceType.ORDER)) {

                return;
            }

            Notification notification = notificationMapper.orderDtoToEntity(orderEventDto);
            Notification savedNotification = notificationRepository.save(notification);
            sendSmsNotification(savedNotification);

        } catch (Exception e) {
            throw new RuntimeException("Failed to process order notification", e);
        }
    }

    @Override
    public void retryFailedNotifications() {

        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsForRetry(NotificationStatus.FAILED);

        for (Notification notification : failedNotifications) {
            try {

                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setStatus(NotificationStatus.RETRY);

                // Send notification based on type
                switch (notification.getNotificationType()) {
                    case SMS -> sendSmsNotification(notification);
                    case EMAIL -> sendEmailNotification(notification);
                    default -> log.warn("Unsupported notification type: {}", notification.getNotificationType());
                }

            } catch (Exception e) {

                notification.setErrorMessage(e.getMessage());
                notification.setStatus(NotificationStatus.FAILED);
                notificationRepository.save(notification);
            }
        }

    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getFailedNotifications() {

        List<Notification> failedNotifications = notificationRepository
                .findByStatusOrderByCreatedAtDesc(NotificationStatus.FAILED);

        return failedNotifications.stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    public void sendSmsNotification(Notification notification) {

        try {
            // Simulate SMS sending - replace with actual SMS service integration
            simulateSmsService(notification);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);

            notificationRepository.save(notification);

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);

            throw new RuntimeException("Failed to send SMS notification", e);
        }
    }

    @Override
    public void sendEmailNotification(Notification notification) {

        try {
            // Simulate email sending - replace with actual email service integration
            simulateEmailService(notification);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
            notificationRepository.save(notification);


        } catch (Exception e) {

            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);

            throw new RuntimeException("Failed to send email notification", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicateNotification(UUID referenceId,ReferenceType referenceType) {
        return notificationRepository.existsByReferenceIdAndReferenceType(referenceId, referenceType);
    }

    private String generatePaymentMessage(PaymentNotificationDto paymentDto) {
        return switch (paymentDto.getStatus()) {
            case COMPLETED -> String.format("Payment of $%.2f %s has been processed successfully. Transaction ID: %s",
                    paymentDto.getAmount(), paymentDto.getCurrency(), paymentDto.getTransactionId());
            case FAILED -> String.format("Payment of $%.2f %s has failed. Please try again.",
                    paymentDto.getAmount(), paymentDto.getCurrency());
            case CANCELLED -> String.format("Payment of $%.2f %s has been cancelled.",
                    paymentDto.getAmount(), paymentDto.getCurrency());
            default -> String.format("Payment status update: %s for amount $%.2f %s",
                    paymentDto.getStatus(), paymentDto.getAmount(), paymentDto.getCurrency());
        };
    }

    private void simulateSmsService(Notification notification) {

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (Math.random() < 0.05) {
            throw new RuntimeException("SMS service temporarily unavailable");
        }

    }

    private void simulateEmailService(Notification notification) {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (Math.random() < 0.03) {
            throw new RuntimeException("Email service temporarily unavailable");
        }

    }
}