package com.nahid.order.consumer;

import com.nahid.order.dto.event.PaymentResultEventDto;
import com.nahid.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${kafka.topic.payment-result:payment-result}",
            groupId = "${spring.kafka.consumer.group-id:order-service-group}",
            containerFactory = "paymentResultKafkaListenerContainerFactory"
    )
    public void handlePaymentResult(
            @Payload PaymentResultEventDto paymentResultEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = "X-Correlation-ID", required = false) String correlationHeader,
            Acknowledgment acknowledgment) {

        String correlationId = (correlationHeader != null && !correlationHeader.isBlank())
                ? correlationHeader : java.util.UUID.randomUUID().toString();
        org.slf4j.MDC.put("correlationId", correlationId);

        try {
            log.info("kafka.message.received - Topic: {}, Partition: {}, Offset: {}, orderId: {}, paymentId: {}, status: {}",
                    topic, partition, offset,
                    paymentResultEvent != null ? paymentResultEvent.getOrderId() : null,
                    paymentResultEvent != null ? paymentResultEvent.getPaymentId() : null,
                    paymentResultEvent != null ? paymentResultEvent.getStatus() : null);

            if (paymentResultEvent == null || paymentResultEvent.getOrderId() == null) {
                log.error("kafka.message.error - PaymentResult event missing required orderId");
                acknowledgment.acknowledge();
                return;
            }

            orderService.processPaymentResult(paymentResultEvent);
            acknowledgment.acknowledge();

            log.info("kafka.message.processed - PaymentResult processed successfully for orderId: {}", paymentResultEvent.getOrderId());

        } catch (Exception e) {
            log.error("kafka.message.error - Error processing PaymentResult for orderId: {}. Error: {}",
                    paymentResultEvent != null ? paymentResultEvent.getOrderId() : "unknown",
                    e.getMessage(), e);
            throw new RuntimeException("Failed to process PaymentResult, delegating to Kafka errorHandler/DLT", e);
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }
    }
}
