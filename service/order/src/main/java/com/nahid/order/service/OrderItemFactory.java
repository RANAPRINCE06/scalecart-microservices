package com.nahid.order.service;

import com.nahid.order.dto.request.CreateOrderItemRequest;
import com.nahid.order.dto.response.PurchaseProductItemResultDto;
import com.nahid.order.entity.OrderItem;
import com.nahid.order.exception.OrderProcessingException;
import com.nahid.order.mapper.OrderMapper;
import com.nahid.order.util.constant.ExceptionMessageConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderItemFactory {

    private final OrderMapper orderMapper;

    public List<OrderItem> createOrderItems(
            List<CreateOrderItemRequest> items,
            Map<Long, PurchaseProductItemResultDto> reservedItemsMap
    ) {
        return items.stream()
                .map(itemRequest -> createOrderItem(itemRequest, reservedItemsMap))
                .toList();
    }

    private OrderItem createOrderItem(CreateOrderItemRequest itemRequest,
                                      Map<Long, PurchaseProductItemResultDto> reservedItemsMap) {
        Long productId = itemRequest.getProductId();
        PurchaseProductItemResultDto reservedItem = reservedItemsMap.get(productId);

        if (reservedItem == null || !reservedItem.isAvailable()) {
            throw new OrderProcessingException(String.format(
                    ExceptionMessageConstant.PRODUCT_RESERVATION_FAILED,
                    "Product unavailable or not reserved: " + productId));
        }

        if (reservedItem.getPrice() == null) {
            throw new OrderProcessingException(String.format(
                    ExceptionMessageConstant.PRODUCT_PRICE_FETCH_FAILED,
                    "Price missing for product: " + productId));
        }

        if (!itemRequest.getQuantity().equals(reservedItem.getRequestedQuantity())) {
            log.warn("Quantity mismatch for product {}. Requested: {}, Reserved: {}",
                    productId, itemRequest.getQuantity(), reservedItem.getRequestedQuantity());
        }

        OrderItem item = orderMapper.toEntity(itemRequest);
        item.setProductName(reservedItem.getProductName());
        item.setProductSku(reservedItem.getSku());
        item.setUnitPrice(reservedItem.getPrice());
        item.setTotalPrice(reservedItem.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

        return item;
    }
}
