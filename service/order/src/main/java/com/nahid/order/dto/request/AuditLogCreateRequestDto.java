package com.nahid.order.dto.request;

public class AuditLogCreateRequestDto {
    private String actionType;
    private String entity;
    private String serviceName;
    private String action;
    private String actionBy;
    private String actionOn;
    private String actionDescription;
    private String actionStatus;
}
