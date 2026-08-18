package com.nahid.notification.entity;

import com.nahid.notification.enums.NotificationStatus;
import com.nahid.notification.enums.NotificationType;
import com.nahid.notification.enums.ReferenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity<UUID> {

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId; // paymentId or orderId

    @Column(name = "reference_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType; // PAYMENT or ORDER

    @Column(name = "customer_id", nullable = false)
    private String userId;

    @Column(name = "customer_email")
    private String userEmail;

    @Column(name = "customer_phone")
    private String userPhone;

    @Column(name = "notification_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;



}
