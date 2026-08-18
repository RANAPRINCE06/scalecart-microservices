package com.nahid.audit.mapper;

import com.nahid.audit.dto.AuditEventDTO;
import com.nahid.audit.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface  AuditMapper {
    @Mapping(target = "id", ignore = true)
    AuditLog toEntity(AuditEventDTO auditEventDTO);

    AuditEventDTO toDto(AuditLog auditLog);

}
