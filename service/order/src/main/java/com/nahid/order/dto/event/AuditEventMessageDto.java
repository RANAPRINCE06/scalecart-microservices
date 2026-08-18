package com.nahid.order.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nahid.order.enums.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditEventMessageDto {
    private String eventId;
    private String eventType;
    private String serviceName;
    private String entityName;
    private String entityId;
    private String userId;
    private String action;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private String ipAddress;
    private EventStatus status;
    private String errorMessage;

    private Map<String, Object> previousState;
    private Map<String, Object> nextState;
}
