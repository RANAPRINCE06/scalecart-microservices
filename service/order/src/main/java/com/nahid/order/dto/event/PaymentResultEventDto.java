package com.nahid.order.dto.event;

import com.nahid.order.enums.PaymentStatus;
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
public class PaymentResultEventDto {

    private UUID orderId;
    private UUID paymentId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String failureReason;
    private LocalDateTime timestamp;
}
