package com.nahid.product.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponseDto {

    Long id;
    String name;
    String description;
    String sku;
    BigDecimal price;
    BigDecimal costPrice;
    Integer stockQuantity;
    Integer minStockLevel;
    CategoryResponseDto category;
    String brand;
    BigDecimal weight;
    String dimensions;
    Boolean isActive;
    Boolean isFeatured;
    String imageUrl;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
