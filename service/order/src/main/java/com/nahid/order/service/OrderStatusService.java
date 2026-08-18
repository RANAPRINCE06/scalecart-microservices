package com.nahid.order.service;

import com.nahid.order.enums.OrderStatus;

public interface OrderStatusService {

    void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus);

    void validateCancellation(OrderStatus currentStatus);
}

