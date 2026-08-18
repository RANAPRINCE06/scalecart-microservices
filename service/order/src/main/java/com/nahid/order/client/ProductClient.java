package com.nahid.order.client;

import com.nahid.order.dto.request.PurchaseProductRequestDto;
import com.nahid.order.dto.response.ApiResponse;
import com.nahid.order.dto.response.PurchaseProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "product-service",
        path = "/api/v1/products",
        fallback = ProductFeignClientFallback.class)
public interface ProductClient {

    @PostMapping("/inventory/reservations")
    ResponseEntity<ApiResponse<PurchaseProductResponseDto>> reserveInventory(
            @RequestBody PurchaseProductRequestDto request);

    @PostMapping("/inventory/reservations/{orderReference}/confirm")
    ResponseEntity<ApiResponse<Void>> confirmReservation(
            @PathVariable("orderReference") String orderReference);

    @PostMapping("/inventory/reservations/{orderReference}/release")
    ResponseEntity<ApiResponse<Void>> releaseReservation(
            @PathVariable("orderReference") String orderReference);
}
