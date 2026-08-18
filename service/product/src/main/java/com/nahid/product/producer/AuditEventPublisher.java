package com.nahid.product.producer;


import com.nahid.product.dto.event.AuditEventMessageDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Data
public class AuditEventPublisher {

    private final KafkaTemplate<String, AuditEventMessageDto> auditKafkaTemplate;
    @Value("${spring.kafka.topic.audit-topic}")
    private String auditTopic;

    public void publishAuditEvent(AuditEventMessageDto auditEventMessageDto) {
        auditKafkaTemplate.send(
                MessageBuilder.withPayload(auditEventMessageDto).setHeader(KafkaHeaders.TOPIC, auditTopic)
                        .setHeader(KafkaHeaders.KEY, auditEventMessageDto.getEventId())
                        .build()
        );
    }

}
