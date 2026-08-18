package com.nahid.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nahid.notification.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventDto {
    private UUID orderId;
    private String orderNumber;
    private String customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String eventType;


}