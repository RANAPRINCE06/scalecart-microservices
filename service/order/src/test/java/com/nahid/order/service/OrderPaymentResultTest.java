package com.nahid.order.service;

import com.nahid.order.consumer.PaymentResultConsumer;
import com.nahid.order.dto.event.PaymentResultEventDto;
import com.nahid.order.entity.Order;
import com.nahid.order.enums.OrderStatus;
import com.nahid.order.enums.PaymentStatus;
import com.nahid.order.mapper.OrderMapper;
import com.nahid.order.producer.OrderEventPublisher;
import com.nahid.order.repository.OrderRepository;
import com.nahid.order.service.impl.OrderServiceImpl;
import com.nahid.order.service.impl.OrderStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentResultTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private ProductPurchaseService productPurchaseService;

    @Mock
    private OrderNumberService orderNumberService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private OrderItemFactory orderItemFactory;

    @Mock
    private Acknowledgment acknowledgment;

    private OrderServiceImpl orderService;
    private PaymentResultConsumer consumer;

    @BeforeEach
    void setUp() {
        OrderStatusService orderStatusService = new OrderStatusServiceImpl();
        orderService = new OrderServiceImpl(
                orderRepository,
                orderMapper,
                userValidationService,
                productPurchaseService,
                orderStatusService,
                orderNumberService,
                orderEventPublisher,
                orderItemFactory
        );
        consumer = new PaymentResultConsumer(orderService);
    }

    @Test
    void testSuccessfulPaymentUpdatesOrderToConfirmed() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Order order = Order.builder()
                .orderNumber("ORD-100")
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(150.00))
                .currency("USD")
                .build();
        order.setOrderId(orderId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResultEventDto event = PaymentResultEventDto.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .status(PaymentStatus.COMPLETED)
                .amount(BigDecimal.valueOf(150.00))
                .build();

        orderService.processPaymentResult(event);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(paymentId, order.getPaymentId());
        verify(orderRepository).save(order);
        verify(orderEventPublisher).publishOrderEvent(any());
    }

    @Test
    void testFailedPaymentUpdatesOrderToCancelledAndReleasesInventory() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Order order = Order.builder()
                .orderNumber("ORD-101")
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200.00))
                .currency("USD")
                .build();
        order.setOrderId(orderId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResultEventDto event = PaymentResultEventDto.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .status(PaymentStatus.FAILED)
                .failureReason("Insufficient funds")
                .build();

        orderService.processPaymentResult(event);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(paymentId, order.getPaymentId());
        verify(productPurchaseService).releaseReservation("ORD-101");
        verify(orderRepository).save(order);
        verify(orderEventPublisher).publishOrderEvent(any());
    }

    @Test
    void testDuplicatePaymentResultIsIdempotent() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        Order order = Order.builder()
                .orderNumber("ORD-102")
                .userId(1L)
                .status(OrderStatus.CONFIRMED)
                .paymentId(paymentId)
                .totalAmount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .build();
        order.setOrderId(orderId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResultEventDto event = PaymentResultEventDto.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .status(PaymentStatus.COMPLETED)
                .build();

        orderService.processPaymentResult(event);

        verify(orderRepository, never()).save(any());
        verify(orderEventPublisher, never()).publishOrderEvent(any());
    }

    @Test
    void testNonexistentOrderHandledSafely() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        PaymentResultEventDto event = PaymentResultEventDto.builder()
                .orderId(orderId)
                .paymentId(UUID.randomUUID())
                .status(PaymentStatus.COMPLETED)
                .build();

        orderService.processPaymentResult(event);

        verify(orderRepository, never()).save(any());
        verify(orderEventPublisher, never()).publishOrderEvent(any());
    }

    @Test
    void testConsumerHandlesPaymentResultSuccess() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Order order = Order.builder().status(OrderStatus.PENDING).build();
        order.setOrderId(orderId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        PaymentResultEventDto event = PaymentResultEventDto.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .status(PaymentStatus.COMPLETED)
                .build();

        consumer.handlePaymentResult(event, "payment-result", 0, 50L, acknowledgment);

        verify(acknowledgment).acknowledge();
    }
}
