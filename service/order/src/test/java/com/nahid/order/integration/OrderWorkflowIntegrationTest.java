package com.nahid.order.integration;

import com.nahid.order.dto.event.PaymentResultEventDto;
import com.nahid.order.entity.Order;
import com.nahid.order.entity.ShippingAddress;
import com.nahid.order.enums.OrderStatus;
import com.nahid.order.enums.PaymentStatus;
import com.nahid.order.repository.OrderRepository;
import com.nahid.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
            .withPassword("1234")
            .withInitScript("init-schema.sql");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "order_schema");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.application.name", () -> "order-service");
        registry.add("kafka.topic.order-notification", () -> "order-notification");
        registry.add("kafka.topic.payment-result", () -> "payment-result");
        registry.add("spring.kafka.topic.audit-topic", () -> "audit-topic");
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Test
    @DisplayName("Integration Test: PaymentResult SUCCESS transitions Order PENDING -> CONFIRMED")
    void testPaymentResultSuccessTransitionsOrderState() {
        ShippingAddress address = ShippingAddress.builder()
                .firstName("Jane")
                .lastName("Doe")
                .streetAddress("123 Main St")
                .city("Springfield")
                .state("IL")
                .country("USA")
                .postalCode("62701")
                .build();

        Order order = Order.builder()
                .userId(1L)
                .orderNumber("ORD-INT-001")
                .totalAmount(new BigDecimal("150.00"))
                .currency("USD")
                .shippingAddress(address)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.saveAndFlush(order);
        UUID orderId = savedOrder.getOrderId();

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
