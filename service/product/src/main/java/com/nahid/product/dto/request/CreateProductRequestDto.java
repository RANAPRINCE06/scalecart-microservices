package com.nahid.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateProductRequestDto {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    String name;

    String description;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    String sku;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimal places")
    BigDecimal price;

    @DecimalMin(value = "0.0", message = "Cost price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Cost price must have at most 2 decimal places")
    BigDecimal costPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be non-negative")
    Integer stockQuantity;

    @Min(value = 0, message = "Minimum stock level must be non-negative")
    Integer minStockLevel;

    @NotNull(message = "Category ID is required")
    Long categoryId;

    String brand;

    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    BigDecimal weight;

    String dimensions;

    Boolean isFeatured = false;

    String imageUrl;

}