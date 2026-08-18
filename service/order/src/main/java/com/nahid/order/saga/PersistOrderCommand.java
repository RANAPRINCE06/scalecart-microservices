package com.nahid.order.saga;

import com.nahid.order.dto.response.PurchaseProductItemResultDto;
import com.nahid.order.dto.response.PurchaseProductResponseDto;
import com.nahid.order.entity.Order;
import com.nahid.order.entity.OrderItem;
import com.nahid.order.enums.OrderStatus;
import com.nahid.order.mapper.OrderMapper;
import com.nahid.order.repository.OrderRepository;
import com.nahid.order.service.OrderItemFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PersistOrderCommand implements SagaCommand {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemFactory orderItemFactory;
    private final OrderSagaContext context;

    public PersistOrderCommand(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            OrderItemFactory orderItemFactory,
            OrderSagaContext context
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.orderItemFactory = orderItemFactory;
        this.context = context;
    }

    @Override
    public void execute() {
        PurchaseProductResponseDto reservationResponse = context.getReservationResponse();
        Map<Long, PurchaseProductItemResultDto> reservedItemsMap = reservationResponse.getItems()
                .stream()
                .collect(Collectors.toMap(
                        PurchaseProductItemResultDto::getProductId,
                        Function.identity(),
                        (existing, replacement) -> existing));

        Order order = orderMapper.toEntity(context.getRequest());
        order.setOrderNumber(context.getOrderNumber());
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> orderItems = orderItemFactory.createOrderItems(
                context.getRequest().getOrderItems(),
                reservedItemsMap);

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
        orderItems.forEach(order::addOrderItem);

        Order savedOrder = orderRepository.save(order);
        context.setOrderItems(orderItems);
        context.setSavedOrder(savedOrder);
    }

    @Override
    public void rollback() {
        Order savedOrder = context.getSavedOrder();
        if (savedOrder != null) {
            orderRepository.delete(savedOrder);
        }
    }
}
