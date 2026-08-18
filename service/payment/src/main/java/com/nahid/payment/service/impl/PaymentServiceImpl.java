package com.nahid.payment.service.impl;

import com.nahid.payment.dto.request.PaymentRequestDto;
import com.nahid.payment.dto.response.PaymentResponseDto;
import com.nahid.payment.entity.Payment;
import com.nahid.payment.enums.PaymentStatus;
import com.nahid.payment.exception.PaymentException;
import com.nahid.payment.exception.PaymentNotFoundException;
import com.nahid.payment.mapper.PaymentMapper;
import com.nahid.payment.producer.PaymentNotificationProducer;
import com.nahid.payment.repository.PaymentRepository;
import com.nahid.payment.service.PaymentService;
import com.nahid.payment.util.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.nahid.payment.util.constant.AppConstant.PAYMENT;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentNotificationProducer notificationProducer;

    @Override
    @Auditable(eventType = "CREATE", entityName = PAYMENT, action = "PROCESS_PAYMENT")
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {

        if (paymentRepository.existsByOrderId(requestDto.getOrderId())) {
            throw new PaymentException("Payment already exists for order: " + requestDto.getOrderId());
        }
        Payment payment = paymentMapper.toEntity(requestDto);
        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            payment = paymentRepository.save(payment);

            boolean paymentSuccess = simulatePaymentGateway(payment);

            if (paymentSuccess) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(generateTransactionId());
                payment.setProcessedAt(LocalDateTime.now());
                payment.setGatewayResponse("Payment processed successfully");

                payment = paymentRepository.save(payment);
                notificationProducer.sendPaymentNotification(payment);

            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment gateway declined the transaction");
                payment.setProcessedAt(LocalDateTime.now());

                payment = paymentRepository.save(payment);

            }

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("System error: " + e.getMessage());
            payment.setProcessedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
        }

        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Auditable(eventType = "UPDATE", entityName = PAYMENT, action = "CANCEL_PAYMENT")
    public PaymentResponseDto cancelPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentException("Cannot cancel completed payment: " + paymentId);
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setProcessedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);


        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Auditable(eventType = "UPDATE", entityName = PAYMENT, action = "REFUND_PAYMENT")
    public PaymentResponseDto refundPayment(UUID paymentId, BigDecimal refundAmount) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException("Cannot refund payment that is not completed: " + paymentId);
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new PaymentException("Refund amount cannot be greater than payment amount");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setProcessedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getUserTotalPaidAmount(Long userId) {
        BigDecimal totalAmount = paymentRepository.getTotalAmountByUserAndStatus(
                userId, PaymentStatus.COMPLETED);

        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getRecentPayments() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Payment> payments = paymentRepository.findRecentPayments(since);

        return payments.stream()
                .map(paymentMapper::toResponseDto)
                .toList();
    }



    @Override
    @Auditable(eventType = "UPDATE", entityName = PAYMENT, action = "RETRY_PAYMENT")
    public PaymentResponseDto retryFailedPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new PaymentException("Can only retry failed payments: " + paymentId);
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setFailureReason(null);
        payment = paymentRepository.save(payment);
        boolean paymentSuccess = simulatePaymentGateway(payment);

        if (paymentSuccess) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(generateTransactionId());
            payment.setProcessedAt(LocalDateTime.now());
            payment.setGatewayResponse("Payment processed successfully on retry");
            payment = paymentRepository.save(payment);

            // Send notification to Kafka
            notificationProducer.sendPaymentNotification(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment gateway declined the transaction on retry");
            payment.setProcessedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

        }

        return paymentMapper.toResponseDto(payment);
    }


    private boolean simulatePaymentGateway(Payment payment) {
        try {
            // Simulate processing delay
            Thread.sleep(100);

            // Simulate 95% success rate
            return Math.random() < 0.95;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));
        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByUserId(Long userId) {

        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return payments.stream()
                .map(paymentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByStatus(PaymentStatus status) {
        List<Payment> payments = paymentRepository.findByStatusOrderByCreatedAtDesc(status);
        return payments.stream()
                .map(paymentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with transaction ID: " + transactionId));

        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Auditable(eventType = "UPDATE", entityName = PAYMENT, action = "UPDATE_PAYMENT_STATUS")
    public PaymentResponseDto updatePaymentStatus(UUID paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        payment.setStatus(status);
        payment.setProcessedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        return paymentMapper.toResponseDto(payment);

    }
}