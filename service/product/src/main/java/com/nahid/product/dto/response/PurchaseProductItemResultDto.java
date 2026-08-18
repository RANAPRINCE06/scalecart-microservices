package com.nahid.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
