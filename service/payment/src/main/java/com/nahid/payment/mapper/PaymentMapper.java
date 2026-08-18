package com.nahid.payment.mapper;


import com.nahid.payment.dto.event.PaymentNotificationDto;
import com.nahid.payment.dto.request.PaymentRequestDto;
import com.nahid.payment.dto.response.PaymentResponseDto;
import com.nahid.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PaymentMapper {

    /**
     * Maps PaymentRequestDto to Payment entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "gatewayResponse", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    Payment toEntity(PaymentRequestDto requestDto);

    /**
     * Maps Payment entity to PaymentResponseDto
     */
    PaymentResponseDto toResponseDto(Payment payment);

    /**
     * Maps Payment entity to PaymentNotificationDto
     */
    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "notificationType", constant = "SMS")
    @Mapping(target = "message", expression = "java(buildNotificationMessage(payment))")
    PaymentNotificationDto toNotificationDto(Payment payment);

    /**
     * Updates existing Payment entity with new data
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Payment payment, PaymentRequestDto requestDto);

    /**
     * Builds notification message for successful payment
     */
    default String buildNotificationMessage(Payment payment) {
        return String.format(
                "Payment of %s %s for order %s has been processed successfully. Transaction ID: %s",
                payment.getAmount(),
                payment.getCurrency(),
                payment.getOrderId(),
                payment.getTransactionId()
        );
    }
}