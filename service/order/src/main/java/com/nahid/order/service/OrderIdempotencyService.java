package com.nahid.order.service;

import com.nahid.order.dto.request.CreateOrderRequest;
import com.nahid.order.dto.request.OrderDto;
import com.nahid.order.entity.OrderIdempotencyRecord;

import java.util.Optional;

public interface OrderIdempotencyService {

    String computeRequestHash(CreateOrderRequest request);

    Optional<OrderDto> getExistingOrder(String idempotencyKey, String requestHash);

    OrderIdempotencyRecord createInProcessRecord(String idempotencyKey, String requestHash);

    void markCompleted(OrderIdempotencyRecord record, OrderDto orderDto);

    void markFailed(OrderIdempotencyRecord record);
}
