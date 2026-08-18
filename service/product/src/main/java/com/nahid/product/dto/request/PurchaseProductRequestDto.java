package com.nahid.product.dto.request;


import jakarta.validation.Valid;
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
public class PurchaseProductRequestDto {

    @NotNull(message = "Product ID cannot be null")
    private String orderReference;

    @NotEmpty(message = "Items cannot be empty for a purchase")
    @Valid
    private List<PurchaseProductItemDto> items;
}
