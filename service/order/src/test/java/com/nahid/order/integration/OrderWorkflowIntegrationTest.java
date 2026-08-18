package com.nahid.order.integration;

import com.nahid.order.dto.event.PaymentResultEventDto;
import com.nahid.order.entity.Order;
import com.nahid.order.enums.OrderStatus;
import com.nahid.order.enums.PaymentStatus;
import com.nahid.order.repository.OrderRepository;
import com.nahid.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OrderWorkflowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ecommerce")
            .withUsername("nahid")
            .withPassword("1234");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Test
    @DisplayName("Integration Test: PaymentResult SUCCESS transitions Order PENDING -> CONFIRMED")
    void testPaymentResultSuccessTransitionsOrderState() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .userId(1L)
                .orderNumber("ORD-INT-001")
                .totalAmount(new BigDecimal("150.00"))
                .status(OrderStatus.PENDING)
                .build();
        order.setOrderId(orderId);

        orderRepository.save(order);

        UUID paymentId = UUID.randomUUID();
        PaymentResultEventDto event = PaymentResultEventDto.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .status(PaymentStatus.COMPLETED)
                .amount(new BigDecimal("150.00"))
                .timestamp(LocalDateTime.now())
                .build();

        orderService.processPaymentResult(event);

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus());
        assertEquals(paymentId, updatedOrder.getPaymentId());
    }
}
