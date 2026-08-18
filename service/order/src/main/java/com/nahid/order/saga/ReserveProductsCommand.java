package com.nahid.order.saga;

import com.nahid.order.dto.response.PurchaseProductResponseDto;
import com.nahid.order.exception.OrderProcessingException;
import com.nahid.order.service.ProductPurchaseService;
import com.nahid.order.util.constant.ExceptionMessageConstant;

public class ReserveProductsCommand implements SagaCommand {

    private final ProductPurchaseService productPurchaseService;
    private final OrderSagaContext context;

    public ReserveProductsCommand(ProductPurchaseService productPurchaseService, OrderSagaContext context) {
        this.productPurchaseService = productPurchaseService;
        this.context = context;
    }

    @Override
    public void execute() {
        PurchaseProductResponseDto reservationResponse =
                productPurchaseService.reserveProducts(context.getRequest(), context.getOrderNumber());

        if (reservationResponse == null || reservationResponse.getItems() == null || reservationResponse.getItems().isEmpty()) {
            throw new OrderProcessingException(String.format(
                    ExceptionMessageConstant.PRODUCT_RESERVATION_FAILED,
                    "Product service returned empty reservation data"));
        }

        context.setReservationResponse(reservationResponse);
    }

    @Override
    public void rollback() {
        productPurchaseService.releaseReservation(context.getOrderNumber());
    }
}
