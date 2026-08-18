package com.nahid.order.producer;

import com.nahid.order.dto.OrderEventDto;
import com.nahid.order.exception.PublishOrderEventException;
import com.nahid.order.util.constant.ExceptionMessageConstant;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEventDto> kafkaTemplate;

    @Value("${kafka.topic.order-notification}")
    private String orderNotificationTopic;
    public void publishOrderEvent(OrderEventDto orderEvent) {
        validateOrderEvent(orderEvent);

        Message<OrderEventDto> message = MessageBuilder
                .withPayload(orderEvent)
                .setHeader(KafkaHeaders.TOPIC, orderNotificationTopic)
                .setHeader(KafkaHeaders.KEY, orderEvent.getOrderId().toString())
                .build();

        CompletableFuture<SendResult<String, OrderEventDto>> future = kafkaTemplate.send(message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish order event to Kafka. OrderId: {}, OrderNumber: {}, Topic: {}", orderEvent.getOrderId(), orderEvent.getOrderNumber(), orderNotificationTopic, ex);
            }
        });
    }

    private void validateOrderEvent(OrderEventDto orderEvent) {
        if (orderEvent == null) {
            throw new PublishOrderEventException(
                    ExceptionMessageConstant.EVENT_PUBLISH_FAILED + ": Order event is null"
            );
        }
        if (orderEvent.getOrderId() == null) {
            throw new PublishOrderEventException(
                    ExceptionMessageConstant.EVENT_PUBLISH_FAILED + ": Order ID is null"
            );
        }
        if (orderEvent.getOrderNumber() == null) {
            throw new PublishOrderEventException(
                    ExceptionMessageConstant.EVENT_PUBLISH_FAILED + ": Order number is null"
            );
        }
    }
}