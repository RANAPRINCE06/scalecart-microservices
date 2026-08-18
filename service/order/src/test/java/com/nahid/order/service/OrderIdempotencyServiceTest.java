package com.nahid.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahid.order.dto.request.CreateOrderItemRequest;
import com.nahid.order.dto.request.CreateOrderRequest;
import com.nahid.order.dto.request.OrderDto;
import com.nahid.order.dto.request.ShippingAddressDto;
import com.nahid.order.entity.Order;
import com.nahid.order.entity.OrderIdempotencyRecord;
import com.nahid.order.enums.OrderStatus;
import com.nahid.order.exception.IdempotencyConflictException;
import com.nahid.order.mapper.OrderMapper;
import com.nahid.order.repository.OrderIdempotencyRepository;
import com.nahid.order.repository.OrderRepository;
import com.nahid.order.service.impl.OrderIdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderIdempotencyServiceTest {

    @Mock
    private OrderIdempotencyRepository idempotencyRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    private OrderIdempotencyServiceImpl idempotencyService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        idempotencyService = new OrderIdempotencyServiceImpl(idempotencyRepository, orderRepository, orderMapper, objectMapper);
    }

    private CreateOrderRequest createSampleRequest(Long userId, String currency) {
        return CreateOrderRequest.builder()
                .userId(userId)
                .currency(currency)
                .shippingAddress(ShippingAddressDto.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .streetAddress("123 Main St")
                        .city("Tech City")
                        .state("NY")
                        .country("USA")
                        .postalCode("10001")
                        .build())
                .orderItems(List.of(
                        CreateOrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .build();
    }

    @Test
    void testComputeRequestHashIsDeterministic() {
        CreateOrderRequest request1 = createSampleRequest(1L, "USD");
        CreateOrderRequest request2 = createSampleRequest(1L, "USD");

        String hash1 = idempotencyService.computeRequestHash(request1);
        String hash2 = idempotencyService.computeRequestHash(request2);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    void testSameKeyAndSamePayloadReturnsExistingOrder() {
        String key = "key-123";
        CreateOrderRequest request = createSampleRequest(1L, "USD");
        String hash = idempotencyService.computeRequestHash(request);

        UUID orderId = UUID.randomUUID();
        OrderIdempotencyRecord record = OrderIdempotencyRecord.builder()
                .idempotencyKey(key)
                .requestHash(hash)
                .orderId(orderId)
                .status("COMPLETED")
                .build();

        Order order = Order.builder().status(OrderStatus.PENDING).build();
        order.setOrderId(orderId);
        OrderDto expectedDto = OrderDto.builder().orderId(orderId).build();

        when(idempotencyRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(expectedDto);

        Optional<OrderDto> result = idempotencyService.getExistingOrder(key, hash);

        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().getOrderId());
    }

    @Test
    void testSameKeyDifferentPayloadThrowsConflictException() {
        String key = "key-123";
        CreateOrderRequest request1 = createSampleRequest(1L, "USD");
        CreateOrderRequest request2 = createSampleRequest(2L, "EUR"); // Different payload

        String hash1 = idempotencyService.computeRequestHash(request1);
        String hash2 = idempotencyService.computeRequestHash(request2);

        OrderIdempotencyRecord record = OrderIdempotencyRecord.builder()
                .idempotencyKey(key)
                .requestHash(hash1)
                .status("COMPLETED")
                .build();

        when(idempotencyRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record));

        IdempotencyConflictException ex = assertThrows(IdempotencyConflictException.class, () ->
                idempotencyService.getExistingOrder(key, hash2)
        );

        assertTrue(ex.getMessage().contains("different request payload"));
    }

    @Test
    void testConcurrentRequestThrowsConflictException() {
        String key = "key-123";
        String hash = "hash-123";

        when(idempotencyRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Unique index violation"));

        assertThrows(IdempotencyConflictException.class, () ->
                idempotencyService.createInProcessRecord(key, hash)
        );
    }

    @Test
    void testMarkFailedDeletesRecordToAllowRetry() {
        UUID recordId = UUID.randomUUID();
        OrderIdempotencyRecord record = OrderIdempotencyRecord.builder()
                .idempotencyKey("key-123")
                .status("IN_PROGRESS")
                .build();
        record.setId(recordId);

        idempotencyService.markFailed(record);

        verify(idempotencyRepository).deleteById(recordId);
    }
}
