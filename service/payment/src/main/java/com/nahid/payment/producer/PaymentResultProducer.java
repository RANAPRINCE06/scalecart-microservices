package com.nahid.payment.producer;

import com.nahid.payment.dto.event.PaymentResultEventDto;
import com.nahid.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentResultProducer {

    private final KafkaTemplate<String, PaymentResultEventDto> paymentResultKafkaTemplate;

    @Value("${kafka.topic.payment-result:payment-result}")
    private String paymentResultTopic;

    public void sendPaymentResult(Payment payment) {
        try {
            PaymentResultEventDto event = PaymentResultEventDto.builder()
                    .orderId(payment.getOrderId())
                    .paymentId(payment.getId())
                    .status(payment.getStatus())
                    .amount(payment.getAmount())
                    .failureReason(payment.getFailureReason())
                    .timestamp(payment.getProcessedAt() != null ? payment.getProcessedAt() : LocalDateTime.now())
                    .build();

            log.info("Publishing PaymentResult event to topic: {}. Order ID: {}, Payment ID: {}, Status: {}",
                    paymentResultTopic, payment.getOrderId(), payment.getId(), payment.getStatus());

            Message<PaymentResultEventDto> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, paymentResultTopic)
                    .setHeader(KafkaHeaders.KEY, payment.getOrderId().toString())
                    .setHeader("paymentId", payment.getId().toString())
                    .setHeader("paymentStatus", payment.getStatus().name())
                    .build();

            CompletableFuture<SendResult<String, PaymentResultEventDto>> future =
                    paymentResultKafkaTemplate.send(message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("PaymentResult event sent successfully for Order ID: {}, Payment ID: {}, Offset: {}",
                            payment.getOrderId(), payment.getId(), result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send PaymentResult event for Order ID: {}, Payment ID: {}. Error: {}",
                            payment.getOrderId(), payment.getId(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error creating/sending PaymentResult event for Order ID: {}, Payment ID: {}: {}",
                    payment.getOrderId(), payment.getId(), e.getMessage(), e);
        }
    }
}
