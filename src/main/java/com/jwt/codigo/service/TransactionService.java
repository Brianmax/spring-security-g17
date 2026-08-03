package com.jwt.codigo.service;

import com.jwt.codigo.dto.common.PageResponse;
import com.jwt.codigo.dto.transaction.TransactionResponse;
import com.jwt.codigo.entity.AccountTransactionEntity;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.TransactionMapper;
import com.jwt.codigo.repository.AccountTransactionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {

    private final AccountTransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransactionMapper transactionMapper;

    public TransactionService(
            AccountTransactionRepository transactionRepository,
            AccountService accountService,
            TransactionMapper transactionMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.transactionMapper = transactionMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> findByAccount(UUID accountId, Pageable pageable) {
        accountService.getEntity(accountId);
        return PageResponse.from(
                transactionRepository.findByAccountId(accountId, pageable).map(transactionMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID transactionId) {
        AccountTransactionEntity transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "TRANSACTION_NOT_FOUND",
                        "Transaction not found"
                ));
        return transactionMapper.toResponse(transaction);
    }
}
