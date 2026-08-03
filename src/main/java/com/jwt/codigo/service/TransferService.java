package com.jwt.codigo.service;

import com.jwt.codigo.dto.transfer.CreateTransferRequest;
import com.jwt.codigo.dto.transfer.TransferResponse;
import com.jwt.codigo.entity.AccountTransactionEntity;
import com.jwt.codigo.entity.BankAccountEntity;
import com.jwt.codigo.entity.TransferEntity;
import com.jwt.codigo.enums.AccountStatus;
import com.jwt.codigo.enums.TransactionType;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.TransferMapper;
import com.jwt.codigo.repository.AccountTransactionRepository;
import com.jwt.codigo.repository.AccountTransferSnapshot;
import com.jwt.codigo.repository.BankAccountRepository;
import com.jwt.codigo.repository.TransferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final BankAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final TransferMapper transferMapper;
    private final ExchangeRateService exchangeRateService;

    public TransferService(
            TransferRepository transferRepository,
            BankAccountRepository accountRepository,
            AccountTransactionRepository transactionRepository,
            TransferMapper transferMapper,
            ExchangeRateService exchangeRateService
    ) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferMapper = transferMapper;
        this.exchangeRateService = exchangeRateService;
    }

    @Transactional
    public TransferResult transfer(CreateTransferRequest request) {
        BigDecimal amount = normalizeAmount(request.amount());
        String idempotencyKey = request.idempotencyKey().trim();
        String description = normalizeDescription(request.description());
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "SAME_ACCOUNT_TRANSFER",
                    "Source and destination accounts must be different"
            );
        }

        TransferEntity existing = transferRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            ensureSameRequest(existing, request, amount, description);
            return new TransferResult(transferMapper.toResponse(existing), false);
        }

        Map<UUID, AccountTransferSnapshot> initialAccounts = snapshotsById(accountRepository.findTransferSnapshots(
                List.of(request.sourceAccountId(), request.destinationAccountId())
        ));
        AccountTransferSnapshot initialSource = requireSnapshot(initialAccounts, request.sourceAccountId());
        AccountTransferSnapshot initialDestination = requireSnapshot(initialAccounts, request.destinationAccountId());
        ensureActive(initialSource, "source");
        ensureActive(initialDestination, "destination");
        ensureSufficientFunds(initialSource, amount);
        ExchangeRateService.CurrencyConversion conversion = exchangeRateService.convert(
                amount,
                initialSource.getCurrency(),
                initialDestination.getCurrency()
        );

        List<BankAccountEntity> lockedAccounts = accountRepository.findAllByIdForUpdate(
                List.of(request.sourceAccountId(), request.destinationAccountId())
        );
        if (lockedAccounts.size() != 2) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Source or destination account not found");
        }

        // A duplicate using the same account pair may have completed while these row locks were being acquired.
        existing = transferRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            ensureSameRequest(existing, request, amount, description);
            return new TransferResult(transferMapper.toResponse(existing), false);
        }

        Map<UUID, BankAccountEntity> byId = accountsById(lockedAccounts);
        BankAccountEntity source = byId.get(request.sourceAccountId());
        BankAccountEntity destination = byId.get(request.destinationAccountId());

        ensureActive(source, "source");
        ensureActive(destination, "destination");
        ensureSufficientFunds(source, amount);

        String reference = "TRF-" + UUID.randomUUID();
        TransferEntity transfer = new TransferEntity(
                source,
                destination,
                amount,
                conversion.destinationAmount(),
                conversion.effectiveRate(),
                conversion.rateDate(),
                conversion.provider(),
                reference,
                idempotencyKey,
                description
        );
        // Flush the unique idempotency claim before any balance change.
        transfer = transferRepository.saveAndFlush(transfer);

        source.debit(amount);
        destination.credit(conversion.destinationAmount());
        transactionRepository.saveAll(List.of(
                new AccountTransactionEntity(
                        source,
                        TransactionType.TRANSFER_OUT,
                        amount,
                        source.getBalance(),
                        reference,
                        description
                ),
                new AccountTransactionEntity(
                        destination,
                        TransactionType.TRANSFER_IN,
                        conversion.destinationAmount(),
                        destination.getBalance(),
                        reference,
                        description
                )
        ));
        transfer.complete();
        transactionRepository.flush();
        transferRepository.flush();
        return new TransferResult(transferMapper.toResponse(transfer), true);
    }

    @Transactional(readOnly = true)
    public TransferResponse findById(UUID transferId) {
        TransferEntity transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND", "Transfer not found"));
        return transferMapper.toResponse(transfer);
    }

    private void ensureActive(BankAccountEntity account, String role) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "ACCOUNT_NOT_ACTIVE",
                    "The " + role + " account must be active"
            );
        }
    }

    private void ensureActive(AccountTransferSnapshot account, String role) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "ACCOUNT_NOT_ACTIVE",
                    "The " + role + " account must be active"
            );
        }
    }

    private void ensureSufficientFunds(BankAccountEntity source, BigDecimal amount) {
        if (source.getBalance().compareTo(amount) < 0) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_FUNDS",
                    "The source account has insufficient funds"
            );
        }
    }

    private void ensureSufficientFunds(AccountTransferSnapshot source, BigDecimal amount) {
        if (source.getBalance().compareTo(amount) < 0) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_FUNDS",
                    "The source account has insufficient funds"
            );
        }
    }

    private Map<UUID, BankAccountEntity> accountsById(List<BankAccountEntity> accounts) {
        return accounts.stream().collect(Collectors.toMap(BankAccountEntity::getId, Function.identity()));
    }

    private Map<UUID, AccountTransferSnapshot> snapshotsById(List<AccountTransferSnapshot> accounts) {
        return accounts.stream().collect(Collectors.toMap(AccountTransferSnapshot::getId, Function.identity()));
    }

    private AccountTransferSnapshot requireSnapshot(Map<UUID, AccountTransferSnapshot> accounts, UUID accountId) {
        AccountTransferSnapshot account = accounts.get(accountId);
        if (account == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Source or destination account not found");
        }
        return account;
    }

    private void ensureSameRequest(
            TransferEntity existing,
            CreateTransferRequest request,
            BigDecimal amount,
            String description
    ) {
        boolean same = existing.getSourceAccount().getId().equals(request.sourceAccountId())
                && existing.getDestinationAccount().getId().equals(request.destinationAccountId())
                && existing.getAmount().compareTo(amount) == 0
                && Objects.equals(existing.getDescription(), description);
        if (!same) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_REUSED",
                    "The idempotency key was already used for a different transfer request"
            );
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TRANSFER_AMOUNT", "Transfer amount must be greater than zero");
        }
        try {
            return amount.setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TRANSFER_AMOUNT_SCALE",
                    "Transfer amount supports at most four decimal places"
            );
        }
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    public record TransferResult(TransferResponse response, boolean created) {
    }
}
