package com.nahid.audit.consumer;

import com.nahid.audit.dto.AuditEventDTO;
import com.nahid.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(topics = "${spring.kafka.topic.audit-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(@Payload AuditEventDTO auditEventDTO,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                       @Header(KafkaHeaders.OFFSET) long offset,
                       Acknowledgment acknowledgment) {
        log.info("Received Audit Event from topic: {}, partition: {}, offset: {},eventId: {}",
                topic, partition, offset, auditEventDTO.getEventId());

        try{
            auditService.processAuditEvent(auditEventDTO);
            acknowledgment.acknowledge();
        }catch (Exception e){
            log.error("Error processing Audit Event for eventId: {}. Error: {}",
                    auditEventDTO.getEventId(), e.getMessage(), e);
            throw e;
        }
    }

}
