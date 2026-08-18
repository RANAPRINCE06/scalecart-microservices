package com.nahid.order.service.impl;

import com.nahid.order.enums.OrderStatus;
import com.nahid.order.exception.OrderProcessingException;
import com.nahid.order.service.OrderStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

@Service
@Slf4j
public class OrderStatusServiceImpl implements OrderStatusService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            entry(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.CANCELLED)),
            entry(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED)),
            entry(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED)),
            entry(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED)),
            entry(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.REFUNDED))
    );

    @Override
    public void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        Set<OrderStatus> allowedStatuses = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());

        if (!allowedStatuses.contains(newStatus)) {
            throw new OrderProcessingException(
                    "Cannot change status of order from " + currentStatus + " to " + newStatus
            );
        }
    }

    @Override
    public void validateCancellation(OrderStatus currentStatus) {
        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
            throw new OrderProcessingException("Cannot cancel order with status: " + currentStatus);
        }
    }
}

