package com.nahid.order.service;

import com.nahid.order.dto.request.CreateOrderRequest;
import com.nahid.order.dto.response.PurchaseProductResponseDto;

public interface ProductPurchaseService {

    PurchaseProductResponseDto reserveProducts(CreateOrderRequest request, String orderReference);

    void confirmReservation(String orderReference);

    void releaseReservation(String orderReference);
}

