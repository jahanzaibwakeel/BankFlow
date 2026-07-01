package com.bankflow.api.service;

import com.bankflow.api.domain.AuditAction;
import com.bankflow.api.domain.AuditLog;
import com.bankflow.api.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String actorUserId, AuditAction action, String resourceType, String resourceId, String details) {
        auditLogRepository.save(new AuditLog(actorUserId, action, resourceType, resourceId, details));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> all(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
