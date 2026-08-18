package com.nahid.order.client;

import com.nahid.order.dto.request.PurchaseProductRequestDto;
import com.nahid.order.dto.response.ApiResponse;
import com.nahid.order.dto.response.PurchaseProductResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
@Component
@Slf4j
public class ProductFeignClientFallback implements ProductClient {

    @Override
    public ResponseEntity<ApiResponse<PurchaseProductResponseDto>> reserveInventory(
            PurchaseProductRequestDto request) {

        ApiResponse<PurchaseProductResponseDto> apiResponse = ApiResponse.<PurchaseProductResponseDto>builder()
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .success(false)
                .message("Product service is unavailable. Unable to reserve inventory.")
                .data(null)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(apiResponse);
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> confirmReservation(String orderReference) {
        return buildUnavailableResponse("Product service is unavailable. Unable to confirm reservation.");
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> releaseReservation(String orderReference) {
        return buildUnavailableResponse("Product service is unavailable. Unable to release reservation.");
    }

    private ResponseEntity<ApiResponse<Void>> buildUnavailableResponse(String message) {
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .success(false)
                .message(message)
                .data(null)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(apiResponse);
    }
}
