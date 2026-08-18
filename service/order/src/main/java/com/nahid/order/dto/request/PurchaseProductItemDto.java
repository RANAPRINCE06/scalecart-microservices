package com.nahid.order.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PurchaseProductItemDto {
    private Long productId;
    private Integer quantity;
}