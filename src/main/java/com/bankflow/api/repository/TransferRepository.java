package com.bankflow.api.repository;

import com.bankflow.api.domain.Transfer;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    @Query("""
        select t from Transfer t
        where t.sourceAccount.owner.id = :userId or t.destinationAccount.owner.id = :userId
        order by t.createdAt desc
        """)
    Page<Transfer> findVisibleToUser(@Param("userId") UUID userId, Pageable pageable);
}
