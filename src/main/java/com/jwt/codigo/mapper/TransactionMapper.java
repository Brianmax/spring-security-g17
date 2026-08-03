package com.jwt.codigo.mapper;

import com.jwt.codigo.dto.transaction.TransactionResponse;
import com.jwt.codigo.entity.AccountTransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(AccountTransactionEntity transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfterTransaction(),
                transaction.getReference(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
