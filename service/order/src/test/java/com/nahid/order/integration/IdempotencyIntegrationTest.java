package com.nahid.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahid.order.dto.request.CreateOrderItemRequest;
import com.nahid.order.dto.request.CreateOrderRequest;
import com.nahid.order.dto.request.OrderDto;
import com.nahid.order.dto.request.ShippingAddressDto;
import com.nahid.order.entity.Order;
import com.nahid.order.entity.OrderIdempotencyRecord;
import com.nahid.order.entity.ShippingAddress;
import com.nahid.order.enums.OrderStatus;
import com.nahid.order.exception.IdempotencyConflictException;
import com.nahid.order.repository.OrderIdempotencyRepository;
import com.nahid.order.repository.OrderRepository;
import com.nahid.order.service.OrderIdempotencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class IdempotencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ecommerce")
            .withUsername("nahid")
            .withPassword("1234")
            .withInitScript("init-schema.sql");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "order_schema");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.application.name", () -> "order-service");
        registry.add("kafka.topic.order-notification", () -> "order-notification");
        registry.add("kafka.topic.payment-result", () -> "payment-result");
        registry.add("spring.kafka.topic.audit-topic", () -> "audit-topic");
    }

    @Autowired
    private OrderIdempotencyService idempotencyService;

    @Autowired
    private OrderIdempotencyRepository idempotencyRepository;

    @Autowired
    private OrderRepository orderRepository;

    private CreateOrderRequest createSampleRequest(Long userId) {
        return CreateOrderRequest.builder()
                .userId(userId)
                .currency("USD")
                .shippingAddress(ShippingAddressDto.builder()
                        .firstName("Jane")
                        .lastName("Doe")
                        .streetAddress("456 Market St")
                        .city("Tech City")
                        .state("CA")
                        .country("USA")
                        .postalCode("90001")
                        .build())
                .orderItems(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(2L)
                                .quantity(1)
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("Integration Test: Duplicate Idempotency-Key with same payload returns cached Order")
    void testDuplicateKeySamePayloadReturnsCachedOrder() {
        String idempotencyKey = "key-" + UUID.randomUUID();
        CreateOrderRequest request = createSampleRequest(10L);
        String hash = idempotencyService.computeRequestHash(request);

        ShippingAddress address = ShippingAddress.builder()
                .firstName("Jane")
                .lastName("Doe")
                .streetAddress("456 Market St")
                .city("Tech City")
                .state("CA")
                .country("USA")
                .postalCode("90001")
                .build();

        Order order = Order.builder()
                .userId(10L)
                .orderNumber("ORD-IDEM-01")
                .totalAmount(new BigDecimal("99.99"))
                .currency("USD")
                .shippingAddress(address)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.saveAndFlush(order);
        UUID orderId = savedOrder.getOrderId();

        OrderIdempotencyRecord record = idempotencyService.createInProcessRecord(idempotencyKey, hash);
        idempotencyService.markCompleted(record, OrderDto.builder().orderId(orderId).orderNumber("ORD-IDEM-01").build());

        Optional<OrderDto> result = idempotencyService.getExistingOrder(idempotencyKey, hash);

        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().getOrderId());
    }

    @Test
    @DisplayName("Integration Test: Duplicate Idempotency-Key with different payload throws 409 Conflict")
    void testDuplicateKeyDifferentPayloadThrowsConflict() {
        String idempotencyKey = "key-" + UUID.randomUUID();
        CreateOrderRequest request1 = createSampleRequest(10L);
        CreateOrderRequest request2 = createSampleRequest(20L); // Different userId

        String hash1 = idempotencyService.computeRequestHash(request1);
        String hash2 = idempotencyService.computeRequestHash(request2);

        OrderIdempotencyRecord record = idempotencyService.createInProcessRecord(idempotencyKey, hash1);
        idempotencyService.markCompleted(record, OrderDto.builder().orderId(UUID.randomUUID()).orderNumber("ORD-IDEM-02").build());

        assertThrows(IdempotencyConflictException.class, () ->
                idempotencyService.getExistingOrder(idempotencyKey, hash2)
        );
    }
}
