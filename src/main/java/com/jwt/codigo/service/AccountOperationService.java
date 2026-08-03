package com.jwt.codigo.service;

import com.jwt.codigo.dto.transaction.MoneyOperationRequest;
import com.jwt.codigo.dto.transaction.TransactionResponse;
import com.jwt.codigo.entity.AccountTransactionEntity;
import com.jwt.codigo.entity.BankAccountEntity;
import com.jwt.codigo.enums.AccountStatus;
import com.jwt.codigo.enums.TransactionType;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.TransactionMapper;
import com.jwt.codigo.repository.AccountTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class AccountOperationService {

    private final AccountService accountService;
    private final AccountTransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public AccountOperationService(
            AccountService accountService,
            AccountTransactionRepository transactionRepository,
            TransactionMapper transactionMapper
    ) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    @Transactional
    public TransactionResponse deposit(UUID accountId, MoneyOperationRequest request) {
        BigDecimal amount = normalizeAmount(request.amount());
        BankAccountEntity account = accountService.getEntityForUpdate(accountId);
        ensureActive(account);
        account.credit(amount);
        AccountTransactionEntity transaction = new AccountTransactionEntity(
                account,
                TransactionType.DEPOSIT,
                amount,
                account.getBalance(),
                newReference("DEP"),
                normalizedDescription(request.description())
        );
        return transactionMapper.toResponse(transactionRepository.saveAndFlush(transaction));
    }

    @Transactional
    public TransactionResponse withdraw(UUID accountId, MoneyOperationRequest request) {
        BigDecimal amount = normalizeAmount(request.amount());
        BankAccountEntity account = accountService.getEntityForUpdate(accountId);
        ensureActive(account);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_FUNDS",
                    "The account has insufficient funds"
            );
        }
        account.debit(amount);
        AccountTransactionEntity transaction = new AccountTransactionEntity(
                account,
                TransactionType.WITHDRAWAL,
                amount,
                account.getBalance(),
                newReference("WDL"),
                normalizedDescription(request.description())
        );
        return transactionMapper.toResponse(transactionRepository.saveAndFlush(transaction));
    }

    private void ensureActive(BankAccountEntity account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "ACCOUNT_NOT_ACTIVE",
                    "Financial operations require an active account"
            );
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero");
        }
        try {
            return amount.setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT_SCALE", "Amount supports at most four decimal places");
        }
    }

    private String newReference(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String normalizedDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
