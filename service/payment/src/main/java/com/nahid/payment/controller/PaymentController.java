package com.nahid.payment.controller;

import com.nahid.payment.dto.request.PaymentRequestDto;
import com.nahid.payment.dto.response.ApiResponse;
import com.nahid.payment.dto.response.PaymentResponseDto;
import com.nahid.payment.enums.PaymentStatus;
import com.nahid.payment.service.PaymentService;
import com.nahid.payment.util.constant.ApiResponseConstant;
import com.nahid.payment.util.constant.AppConstant;
import com.nahid.payment.util.helper.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDto>> processPayment(@Valid @RequestBody PaymentRequestDto requestDto) {
        PaymentResponseDto response = paymentService.processPayment(requestDto);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.CREATE_SUCCESSFUL, AppConstant.PAYMENT),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentById(@PathVariable UUID paymentId) {
        PaymentResponseDto response = paymentService.getPaymentById(paymentId);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.PAYMENT)
        );
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentByOrderId(@PathVariable UUID orderId) {
        PaymentResponseDto response = paymentService.getPaymentByOrderId(orderId);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.PAYMENT)
        );
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentByTransactionId(@PathVariable String transactionId) {
        PaymentResponseDto response = paymentService.getPaymentByTransactionId(transactionId);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.PAYMENT)
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getPaymentsByCustomerId(@PathVariable Long userId) {
        List<PaymentResponseDto> payments = paymentService.getPaymentsByUserId(userId);
        return ApiResponseUtil.success(
                payments,
                String.format(ApiResponseConstant.FETCH_ALL_SUCCESSFUL, AppConstant.USER_PAYMENTS)
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        List<PaymentResponseDto> payments = paymentService.getPaymentsByStatus(status);
        return ApiResponseUtil.success(
                payments,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, status.name() + " payments")
        );
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getRecentPayments() {
        List<PaymentResponseDto> payments = paymentService.getRecentPayments();
        return ApiResponseUtil.success(
                payments,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.RECENT_PAYMENTS)
        );
    }

    @GetMapping("/User/{userId}/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getUserTotalPaidAmount(@PathVariable Long userId) {
        BigDecimal totalAmount = paymentService.getUserTotalPaidAmount(userId);
        return ApiResponseUtil.success(
                totalAmount,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.TOTAL_PAID_AMOUNT)
        );
    }

    @PutMapping("/{paymentId}/status")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> updatePaymentStatus(
            @PathVariable UUID paymentId,
            @RequestParam PaymentStatus status) {
        PaymentResponseDto response = paymentService.updatePaymentStatus(paymentId, status);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.STATUS_UPDATE_SUCCESSFUL, AppConstant.PAYMENT, status.name())
        );
    }

    @PutMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> cancelPayment(@PathVariable UUID paymentId) {
        PaymentResponseDto response = paymentService.cancelPayment(paymentId);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.ACTION_SUCCESSFUL, AppConstant.PAYMENT, AppConstant.CANCELLED)
        );
    }

    @PutMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> refundPayment(
            @PathVariable UUID paymentId,
            @RequestParam BigDecimal refundAmount) {
        PaymentResponseDto response = paymentService.refundPayment(paymentId, refundAmount);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.ACTION_SUCCESSFUL, AppConstant.PAYMENT, AppConstant.REFUNDED)
        );
    }

    @PutMapping("/{paymentId}/retry")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> retryFailedPayment(@PathVariable UUID paymentId) {
        PaymentResponseDto response = paymentService.retryFailedPayment(paymentId);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.ACTION_SUCCESSFUL, AppConstant.PAYMENT, AppConstant.RETRIED)
        );
    }
}
