package com.bankflow.api.repository;

import com.bankflow.api.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, java.util.UUID> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
