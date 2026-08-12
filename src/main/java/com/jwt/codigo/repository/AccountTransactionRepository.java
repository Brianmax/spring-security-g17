package com.jwt.codigo.repository;

import com.jwt.codigo.entity.AccountTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountTransactionRepository extends JpaRepository<AccountTransactionEntity, UUID> {

    boolean existsByIdAndAccountOwnerId(UUID id, UUID ownerId);

    Page<AccountTransactionEntity> findByAccountId(UUID accountId, Pageable pageable);

    long countByAccountId(UUID accountId);
}
