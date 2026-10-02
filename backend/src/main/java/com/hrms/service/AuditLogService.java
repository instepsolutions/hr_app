package com.hrms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.entity.AuditLog;
import com.hrms.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void record(String entityName, Long entityId, String actionType, Object oldData, Object newData) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setActionType(actionType);
        auditLog.setOldData(serialize(oldData));
        auditLog.setNewData(serialize(newData));
        auditLog.setPerformedBy(currentActor());
        auditLog.setPerformedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    private String serialize(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Audit data could not be serialized", ex);
        }
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }
}
