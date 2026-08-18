package com.nahid.payment.service;

import com.nahid.payment.dto.request.PaymentRequestDto;
import com.nahid.payment.dto.response.PaymentResponseDto;
import com.nahid.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentService {


    PaymentResponseDto processPayment(PaymentRequestDto requestDto);

    PaymentResponseDto getPaymentById(UUID paymentId);

    PaymentResponseDto getPaymentByOrderId(UUID orderId);

    List<PaymentResponseDto> getPaymentsByUserId(Long userId);

    List<PaymentResponseDto> getPaymentsByStatus(PaymentStatus status);

    PaymentResponseDto getPaymentByTransactionId(String transactionId);

    PaymentResponseDto updatePaymentStatus(UUID paymentId, PaymentStatus status);

    PaymentResponseDto cancelPayment(UUID paymentId);

    PaymentResponseDto refundPayment(UUID paymentId, BigDecimal refundAmount);

    BigDecimal getUserTotalPaidAmount(Long userId);

    List<PaymentResponseDto> getRecentPayments();


    PaymentResponseDto retryFailedPayment(UUID paymentId);
}