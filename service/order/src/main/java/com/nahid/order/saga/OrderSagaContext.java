package com.nahid.order.saga;

import com.nahid.order.dto.request.CreateOrderRequest;
import com.nahid.order.dto.response.PurchaseProductResponseDto;
import com.nahid.order.entity.Order;
import com.nahid.order.entity.OrderItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderSagaContext {

    private final CreateOrderRequest request;
    private final String orderNumber;
    private PurchaseProductResponseDto reservationResponse;
    private Order savedOrder;
    private List<OrderItem> orderItems;

    public OrderSagaContext(CreateOrderRequest request, String orderNumber) {
        this.request = request;
        this.orderNumber = orderNumber;
    }
}
