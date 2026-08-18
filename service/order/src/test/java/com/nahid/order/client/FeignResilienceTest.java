package com.nahid.order.client;

import com.nahid.order.dto.request.PurchaseProductRequestDto;
import com.nahid.order.dto.response.ApiResponse;
import com.nahid.order.dto.response.PurchaseProductResponseDto;
import com.nahid.order.dto.response.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class FeignResilienceTest {

    private final ProductFeignClientFallback productFallback = new ProductFeignClientFallback();
    private final UserFeignClientFallback userFallback = new UserFeignClientFallback();

    @Test
    void testProductFallbackReturns503ServiceUnavailable() {
        PurchaseProductRequestDto request = PurchaseProductRequestDto.builder()
                .orderReference("ORD-TEST")
                .build();

        ResponseEntity<ApiResponse<PurchaseProductResponseDto>> response = productFallback.reserveInventory(request);

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("unavailable"));
    }

    @Test
    void testUserFallbackReturns503ServiceUnavailable() {
        ResponseEntity<ApiResponse<UserResponseDto>> response = userFallback.getUserById(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("unavailable"));
    }
}
