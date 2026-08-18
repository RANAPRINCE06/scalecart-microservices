package com.nahid.order.dto.response;

import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;

@Getter
@Setter
public class PurchaseProductItemResultDto {
    private Long productId;
    private String productName;
    private String sku;
    private Integer requestedQuantity;
    private Integer availableQuantity;
    private BigDecimal price;
    private boolean available;
    private String message;
}
