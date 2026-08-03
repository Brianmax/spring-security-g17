package com.jwt.codigo.repository;

import com.jwt.codigo.entity.BankAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccountEntity, UUID> {

    Page<BankAccountEntity> findByOwnerId(UUID ownerId, Pageable pageable);

    boolean existsByOwnerId(UUID ownerId);

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccountEntity a where a.id = :id")
    Optional<BankAccountEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccountEntity a where a.id in :ids order by a.id")
    List<BankAccountEntity> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

    @Query("""
            select a.id as id, a.status as status, a.balance as balance, a.currency as currency
            from BankAccountEntity a
            where a.id in :ids
            """)
    List<AccountTransferSnapshot> findTransferSnapshots(@Param("ids") Collection<UUID> ids);
}
