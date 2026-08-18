package com.nahid.order.mapper;


import com.nahid.order.dto.request.*;
import com.nahid.order.entity.Order;
import com.nahid.order.entity.OrderItem;
import com.nahid.order.entity.ShippingAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    OrderDto toDto(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "orderItemId", source = "id")
    OrderItemDto toDto(OrderItem orderItem);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productSku", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OrderItem toEntity(CreateOrderItemRequest request);

    ShippingAddressDto toDto(ShippingAddress shippingAddress);

    ShippingAddress toEntity(ShippingAddressDto dto);

    void updateOrderFromDto(OrderDto orderDto, @MappingTarget Order order);
}
