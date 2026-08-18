package com.nahid.payment.producer;

import com.nahid.payment.dto.event.AuditEventMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final KafkaTemplate<String, AuditEventMessageDto> auditKafkaTemplate;

    @Value("${spring.kafka.topic.audit-topic}")
    private String auditTopic;

    public void publishAuditEvent(AuditEventMessageDto auditEventMessageDto) {
        auditKafkaTemplate.send(
                MessageBuilder.withPayload(auditEventMessageDto)
                        .setHeader(KafkaHeaders.TOPIC, auditTopic)
                        .setHeader(KafkaHeaders.KEY, auditEventMessageDto.getEventId())
                        .build()
        );
    }
}
