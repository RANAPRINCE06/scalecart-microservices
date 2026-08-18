package com.nahid.order.service.impl;

import com.nahid.order.client.ProductClient;
import com.nahid.order.dto.request.CreateOrderRequest;
import com.nahid.order.dto.request.PurchaseProductItemDto;
import com.nahid.order.dto.request.PurchaseProductRequestDto;
import com.nahid.order.dto.response.ApiResponse;
import com.nahid.order.dto.response.PurchaseProductResponseDto;
import com.nahid.order.exception.OrderProcessingException;
import com.nahid.order.service.ProductPurchaseService;
import com.nahid.order.util.constant.ExceptionMessageConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductPurchaseServiceImpl implements ProductPurchaseService {

    private final ProductClient productClient;

    @Override
    public PurchaseProductResponseDto reserveProducts(CreateOrderRequest request, String orderReference) {

        PurchaseProductRequestDto purchaseRequest = PurchaseProductRequestDto.builder()
                .orderReference(orderReference)
                .items(request.getOrderItems().stream()
                        .map(item -> PurchaseProductItemDto.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();

        ResponseEntity<ApiResponse<PurchaseProductResponseDto>> responseEntity = productClient.reserveInventory(purchaseRequest);
        ApiResponse<PurchaseProductResponseDto> apiResponse =
                responseEntity != null ? responseEntity.getBody() : null;

        if (apiResponse == null) {
            throw new OrderProcessingException(String.format(
                    ExceptionMessageConstant.PRODUCT_RESERVATION_FAILED,
                    "No response received from product service"));
        }

        if (!apiResponse.isSuccess()) {
            String failureMessage = apiResponse.getMessage() != null
                    ? apiResponse.getMessage()
                    : "Product service returned an unsuccessful response";
            throw new OrderProcessingException(String.format(
                    ExceptionMessageConstant.PRODUCT_RESERVATION_FAILED,
                    failureMessage));
        }

        PurchaseProductResponseDto responseData = apiResponse.getData();
        if (responseData == null) {
            throw new OrderProcessingException(String.format(
                    ExceptionMessageConstant.PRODUCT_RESERVATION_FAILED,
                    "Product service returned empty reservation data"));
        }

        if (responseData.getItems() == null || responseData.getItems().isEmpty()) {
            throw new OrderProcessingException(String.format(
                    ExceptionMessageConstant.PRODUCT_RESERVATION_FAILED,
                    "Product service returned empty reservation items"));
        }

        return responseData;
    }

    @Override
    public void confirmReservation(String orderReference) {
        productClient.confirmReservation(orderReference);
    }

    @Override
    public void releaseReservation(String orderReference) {
        productClient.releaseReservation(orderReference);
    }

}

