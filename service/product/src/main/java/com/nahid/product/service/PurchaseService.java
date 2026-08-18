package com.nahid.product.service;

import com.nahid.product.dto.request.PurchaseProductRequestDto;
import com.nahid.product.dto.response.PurchaseProductResponseDto;

public interface PurchaseService {

    PurchaseProductResponseDto reserveInventory(PurchaseProductRequestDto request);

    void confirmReservation(String orderReference);

    void releaseReservation(String orderReference);
}

