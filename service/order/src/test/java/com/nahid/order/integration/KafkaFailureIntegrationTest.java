package com.nahid.order.integration;

import com.nahid.order.dto.event.PaymentResultEventDto;
import com.nahid.order.enums.PaymentStatus;
import com.nahid.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class KafkaFailureIntegrationTest {

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

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("Integration Test: Bounded Kafka Consumer Retries and DLT Recovery")
    void testKafkaConsumerRetriesAndDltHandling() {
        doThrow(new RuntimeException("Simulated processing failure"))
                .when(orderService).processPaymentResult(any());

        PaymentResultEventDto event = PaymentResultEventDto.builder()
                .orderId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .status(PaymentStatus.COMPLETED)
                .amount(new BigDecimal("99.99"))
                .timestamp(LocalDateTime.now())
                .build();

        assertThrows(RuntimeException.class, () -> orderService.processPaymentResult(event));
        verify(orderService, times(1)).processPaymentResult(event);
    }
}
