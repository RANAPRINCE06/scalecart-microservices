package com.nahid.payment.aspects;

import com.nahid.payment.dto.event.AuditEventMessageDto;
import com.nahid.payment.enums.EventStatus;
import com.nahid.payment.producer.AuditEventPublisher;
import com.nahid.payment.util.annotation.Auditable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditEventPublisher auditEventPublisher;
    private final HttpServletRequest request;

    @Value("${spring.application.name}")
    private String serviceName;

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = null;
        try {
            result = joinPoint.proceed();
            auditEventPublisher.publishAuditEvent(
                    buildEventMessageDto(result, auditable, EventStatus.SUCCESS, null)
            );
            return result;
        } catch (Exception e) {
            auditEventPublisher.publishAuditEvent(
                    buildEventMessageDto(result, auditable, EventStatus.FAILED, e.getMessage())
            );
            throw e;
        }
    }

    private AuditEventMessageDto buildEventMessageDto(Object result, Auditable auditable,
                                                      EventStatus eventStatus, String errorMessage) {
        return AuditEventMessageDto.builder()
                .eventId(UUID.randomUUID().toString())
                .entityId(extractEntityId(result))
                .userId(getUserId())
                .ipAddress(getIpAddress())
                .serviceName(serviceName)
                .status(eventStatus)
                .eventType(auditable.eventType())
                .entityName(auditable.entityName())
                .action(auditable.action())
                .errorMessage(errorMessage)
                .build();
    }

    private String getIpAddress() {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            return request.getRemoteAddr();
        }
        return ipAddress.split(",")[0].trim();
    }

    private String extractEntityId(Object result) {
        if (result == null) {
            return "";
        }
        try {
            if (result instanceof Long || result instanceof String || result instanceof UUID) {
                return result.toString();
            }
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            return id != null ? id.toString() : "";
        } catch (Exception e) {
            log.warn("Could not extract ID from result of type {}", result.getClass().getSimpleName(), e);
            return "";
        }
    }

    private String getUserId() {
        String userHeader = request.getHeader("X-Auth-User");
        return userHeader != null ? userHeader : "";
    }
}
