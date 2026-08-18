package com.nahid.order.dto;

import com.nahid.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventDto {

    private UUID orderId;
    private String orderNumber;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    //private List<OrderItemEventDto> orderItems;
    private LocalDateTime createdAt;
    private String eventType; // ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED
}
