package com.nahid.payment.producer;

import com.nahid.payment.dto.event.PaymentNotificationDto;
import com.nahid.payment.entity.Payment;
import com.nahid.payment.enums.PaymentStatus;
import com.nahid.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationProducer {

    private final KafkaTemplate<String, PaymentNotificationDto> kafkaTemplate;
    private final PaymentMapper paymentMapper;

    @Value("${kafka.topic.payment-notification}")
    private String paymentNotificationTopic;


    public void sendPaymentNotification(Payment payment) {

        try {
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                log.debug("Skipping notification for non-completed payment: {}", payment.getId());
                return;
            }

            PaymentNotificationDto notification = paymentMapper.toNotificationDto(payment);

            log.info("Sending payment notification to Kafka. Payment ID: {}, Topic: {}",
                    payment.getId(), paymentNotificationTopic);

            Message<PaymentNotificationDto> message = MessageBuilder
                    .withPayload(notification)
                    .setHeader("paymentId", payment.getId().toString())
                    .setHeader("paymentStatus", payment.getStatus().name())
                    .setHeader(KafkaHeaders.TOPIC, paymentNotificationTopic)
                    .build();

            CompletableFuture<SendResult<String, PaymentNotificationDto>> future = kafkaTemplate.send(message);

            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.info("Payment notification sent successfully. Payment ID: {}, Offset: {}",
                            payment.getId(), result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send payment notification. Payment ID: {}, Error: {}",
                            payment.getId(), exception.getMessage(), exception);
                }
            });
        }
        catch (Exception e) {
            log.error("Error sending payment notification for payment ID {}: {}", payment.getId(), e.getMessage(), e);
        }
    }


    public void sendCustomNotification(String key, PaymentNotificationDto message) {
        log.info("Sending custom notification to Kafka. Key: {}, Topic: {}",
                key, paymentNotificationTopic);

        CompletableFuture<SendResult<String, PaymentNotificationDto>> future = kafkaTemplate.send(
                paymentNotificationTopic,
                key,
                message
        );

        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("Custom notification sent successfully. Key: {}, Offset: {}",
                        key, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send custom notification. Key: {}, Error: {}",
                        key, exception.getMessage(), exception);
            }
        });
    }
}