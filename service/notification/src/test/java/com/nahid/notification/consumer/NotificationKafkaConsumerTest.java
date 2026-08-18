package com.nahid.notification.consumer;

import com.nahid.notification.dto.OrderEventDto;
import com.nahid.notification.dto.PaymentNotificationDto;
import com.nahid.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Acknowledgment acknowledgment;

    private NotificationKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationKafkaConsumer(notificationService);
    }

    @Test
    void testHandlePaymentNotificationSuccess() {
        PaymentNotificationDto dto = new PaymentNotificationDto();
        dto.setPaymentId(UUID.randomUUID());
        dto.setCustomerId("cust-123");
        dto.setAmount(BigDecimal.TEN);

        consumer.handlePaymentNotification(dto, "payment-notification", 0, 100L, acknowledgment);

        verify(notificationService).processPaymentNotification(dto);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void testHandlePaymentNotificationFailureRethrowsException() {
        PaymentNotificationDto dto = new PaymentNotificationDto();
        dto.setPaymentId(UUID.randomUUID());
        dto.setCustomerId("cust-123");
        dto.setAmount(BigDecimal.TEN);

        doThrow(new RuntimeException("DB error")).when(notificationService).processPaymentNotification(dto);

        assertThrows(RuntimeException.class, () ->
                consumer.handlePaymentNotification(dto, "payment-notification", 0, 100L, acknowledgment)
        );

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void testHandleOrderNotificationSuccess() {
        OrderEventDto dto = new OrderEventDto();
        dto.setOrderId(UUID.randomUUID());
        dto.setCustomerId("cust-123");
        dto.setEventType("ORDER_CREATED");

        consumer.handleOrderNotification(null, dto, "order-notification", 0, 100L, acknowledgment);

        verify(notificationService).processOrderNotification(dto);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void testHandleOrderNotificationFailureRethrowsException() {
        OrderEventDto dto = new OrderEventDto();
        dto.setOrderId(UUID.randomUUID());
        dto.setCustomerId("cust-123");
        dto.setEventType("ORDER_CREATED");

        doThrow(new RuntimeException("Mail error")).when(notificationService).processOrderNotification(dto);

        assertThrows(RuntimeException.class, () ->
                consumer.handleOrderNotification(null, dto, "order-notification", 0, 100L, acknowledgment)
        );

        verify(acknowledgment, never()).acknowledge();
    }
}
