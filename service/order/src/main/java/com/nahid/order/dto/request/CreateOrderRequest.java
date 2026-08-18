package com.nahid.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Currency is required")
    private String currency;

    @Valid
    @NotNull(message = "Shipping address is required")
    private ShippingAddressDto shippingAddress;

    @Valid
    @NotEmpty(message = "Order items cannot be empty")
    private List<CreateOrderItemRequest> orderItems;
}