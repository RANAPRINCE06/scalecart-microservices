package com.nahid.audit.service;

import com.nahid.audit.dto.AuditEventDTO;
import com.nahid.audit.mapper.AuditMapper;
import com.nahid.audit.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@RequiredArgsConstructor
@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final AuditMapper auditMapper;


    public void processAuditEvent(AuditEventDTO auditEventDTO){
        auditRepository.save(auditMapper.toEntity(auditEventDTO));
    }
}
