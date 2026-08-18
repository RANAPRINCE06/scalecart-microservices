package com.nahid.payment.service;

import com.nahid.payment.dto.request.PaymentRequestDto;
import com.nahid.payment.dto.response.PaymentResponseDto;
import com.nahid.payment.entity.Payment;
import com.nahid.payment.enums.PaymentMethod;
import com.nahid.payment.enums.PaymentStatus;
import com.nahid.payment.mapper.PaymentMapper;
import com.nahid.payment.producer.PaymentNotificationProducer;
import com.nahid.payment.producer.PaymentResultProducer;
import com.nahid.payment.repository.PaymentRepository;
import com.nahid.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentNotificationProducer notificationProducer;

    @Mock
    private PaymentResultProducer paymentResultProducer;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, paymentMapper, notificationProducer, paymentResultProducer);
    }

    @Test
    void testProcessPaymentPublishesPaymentResultEvent() {
        UUID orderId = UUID.randomUUID();
        PaymentRequestDto requestDto = PaymentRequestDto.builder()
                .orderId(orderId)
                .customerId("cust-100")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .customerEmail("test@example.com")
                .build();

        Payment initialPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(1L)
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PROCESSING)
                .build();

        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentMapper.toEntity(requestDto)).thenReturn(initialPayment);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toResponseDto(any(Payment.class))).thenReturn(PaymentResponseDto.builder().build());

        PaymentResponseDto response = paymentService.processPayment(requestDto);

        assertNotNull(response);
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(paymentResultProducer, times(1)).sendPaymentResult(any(Payment.class));
    }
}
