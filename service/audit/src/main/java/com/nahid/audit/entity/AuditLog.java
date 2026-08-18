package com.nahid.audit.entity;


import com.nahid.audit.enums.EventStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;


@Data
@Table(name="audit_log",indexes = {
        @Index(columnList = "event_id"),
        @Index(columnList = "event_type"),
        @Index(columnList = "user_id")
})
@Entity
public class AuditLog {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="event_id",nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name="entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name="entity_id")
    private String entityId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String action;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address")
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_state", columnDefinition = "jsonb")
    private Map<String, Object> previousState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_state", columnDefinition = "jsonb")
    private Map<String, Object> nextState;

}
