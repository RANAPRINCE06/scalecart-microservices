package com.nahid.order.saga;

import com.nahid.order.service.ProductPurchaseService;

public class ConfirmReservationCommand implements SagaCommand {

    private final ProductPurchaseService productPurchaseService;
    private final OrderSagaContext context;

    public ConfirmReservationCommand(ProductPurchaseService productPurchaseService, OrderSagaContext context) {
        this.productPurchaseService = productPurchaseService;
        this.context = context;
    }

    @Override
    public void execute() {
        productPurchaseService.confirmReservation(context.getOrderNumber());
    }

    @Override
    public void rollback() {
        try {
            productPurchaseService.releaseReservation(context.getOrderNumber());
        } catch (Exception e) {
            // Idempotent - may already be released
        }
    }
}
