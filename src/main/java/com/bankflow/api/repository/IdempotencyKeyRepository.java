package com.bankflow.api.repository;

import com.bankflow.api.domain.IdempotencyKey;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, java.util.UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from IdempotencyKey k where k.userId = :userId and k.idempotencyKey = :key and k.operation = :operation")
    Optional<IdempotencyKey> findForUpdate(@Param("userId") String userId, @Param("key") String key, @Param("operation") String operation);
}
