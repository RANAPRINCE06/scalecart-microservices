package com.nahid.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nahid.notification.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotificationDto {
    private UUID paymentId;
    private UUID orderId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String customerEmail;
    private String customerPhone;
    private String transactionId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    private String notificationType = "SMS";
    private String message;


}
