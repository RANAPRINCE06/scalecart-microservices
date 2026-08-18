package com.nahid.notification.mapper;

import com.nahid.notification.dto.NotificationDto;
import com.nahid.notification.dto.NotificationResponseDto;
import com.nahid.notification.dto.OrderEventDto;
import com.nahid.notification.dto.PaymentNotificationDto;
import com.nahid.notification.entity.Notification;
import com.nahid.notification.enums.NotificationStatus;
import com.nahid.notification.enums.NotificationType;
import com.nahid.notification.enums.ReferenceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDto toDto(Notification notification);

    Notification toEntity(NotificationDto notificationDto);

    @Mapping(target = "referenceType", expression = "java(mapReferenceType(notification.getReferenceType()))")
    @Mapping(target = "notificationType", expression = "java(mapNotificationType(notification.getNotificationType()))")
    @Mapping(target = "status", expression = "java(mapStatus(notification.getStatus()))")
    NotificationResponseDto toResponseDto(Notification notification);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceId", source = "paymentId")
    @Mapping(target = "referenceType", constant = "PAYMENT")
    @Mapping(target = "notificationType", expression = "java(mapNotificationTypeFromString(paymentDto.getNotificationType()))")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Notification paymentDtoToEntity(PaymentNotificationDto paymentDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referenceId", source = "orderId")
    @Mapping(target = "referenceType", constant = "ORDER")
    @Mapping(target = "notificationType", constant = "SMS")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "amount", source = "totalAmount")
    @Mapping(target = "currency", constant = "USD")
    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "userPhone", ignore = true)
    @Mapping(target = "processedAt", source = "createdAt")
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "message", expression = "java(generateOrderMessage(orderDto))")
    Notification orderDtoToEntity(OrderEventDto orderDto);

    void updateNotificationFromDto(NotificationDto dto, @MappingTarget Notification notification);

    // Helper methods for mapping enums to strings
    default String mapReferenceType(ReferenceType referenceType) {
        return referenceType != null ? referenceType.name() : null;
    }

    default String mapNotificationType(NotificationType notificationType) {
        return notificationType != null ? notificationType.name() : null;
    }

    default String mapStatus(NotificationStatus status) {
        return status != null ? status.name() : null;
    }

    default NotificationType mapNotificationTypeFromString(String notificationType) {
        if (notificationType == null) return NotificationType.SMS;
        return switch (notificationType.toUpperCase()) {
            case "EMAIL" -> NotificationType.EMAIL;
            case "PUSH" -> NotificationType.PUSH;
            default -> NotificationType.SMS;
        };
    }

    default String generateOrderMessage(OrderEventDto orderDto) {
        return switch (orderDto.getEventType()) {
            case "ORDER_CREATED" -> String.format("Your order %s has been created successfully. Total amount: $%.2f",
                    orderDto.getOrderNumber(), orderDto.getTotalAmount());
            case "ORDER_UPDATED" -> String.format("Your order %s has been updated. Status: %s",
                    orderDto.getOrderNumber(), orderDto.getStatus());
            case "ORDER_CANCELLED" -> String.format("Your order %s has been cancelled.",
                    orderDto.getOrderNumber());
            default -> String.format("Order %s update: %s", orderDto.getOrderNumber(), orderDto.getStatus());
        };
    }
}