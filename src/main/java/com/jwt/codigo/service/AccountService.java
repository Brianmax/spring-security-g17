package com.jwt.codigo.service;

import com.jwt.codigo.dto.account.AccountResponse;
import com.jwt.codigo.dto.account.CreateAccountRequest;
import com.jwt.codigo.dto.common.PageResponse;
import com.jwt.codigo.entity.BankAccountEntity;
import com.jwt.codigo.entity.UserEntity;
import com.jwt.codigo.enums.AccountStatus;
import com.jwt.codigo.enums.UserStatus;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.AccountMapper;
import com.jwt.codigo.repository.BankAccountRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long ACCOUNT_NUMBER_SPACE = 1_000_000_000_000_000L;

    private final BankAccountRepository accountRepository;
    private final UserService userService;
    private final AccountMapper accountMapper;

    public AccountService(
            BankAccountRepository accountRepository,
            UserService userService,
            AccountMapper accountMapper
    ) {
        this.accountRepository = accountRepository;
        this.userService = userService;
        this.accountMapper = accountMapper;
    }

    @Transactional
    public AccountResponse create(UUID userId, CreateAccountRequest request) {
        UserEntity owner = userService.getEntity(userId);
        if (owner.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "USER_INACTIVE",
                    "Inactive users cannot open new accounts"
            );
        }
        BankAccountEntity account = new BankAccountEntity(
                generateUniqueAccountNumber(),
                request.accountType(),
                request.currency(),
                owner
        );
        return accountMapper.toResponse(accountRepository.saveAndFlush(account));
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> findByOwner(UUID userId, Pageable pageable) {
        userService.getEntity(userId);
        return PageResponse.from(accountRepository.findByOwnerId(userId, pageable).map(accountMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID accountId) {
        return accountMapper.toResponse(getEntity(accountId));
    }

    @Transactional
    public AccountResponse freeze(UUID accountId) {
        BankAccountEntity account = getEntityForUpdate(accountId);
        ensureNotClosed(account);
        if (account.getStatus() == AccountStatus.ACTIVE) {
            account.freeze();
        }
        return accountMapper.toResponse(accountRepository.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse unfreeze(UUID accountId) {
        BankAccountEntity account = getEntityForUpdate(accountId);
        ensureNotClosed(account);
        if (account.getStatus() == AccountStatus.FROZEN) {
            account.unfreeze();
        }
        return accountMapper.toResponse(accountRepository.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse close(UUID accountId) {
        BankAccountEntity account = getEntityForUpdate(accountId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            return accountMapper.toResponse(account);
        }
        if (account.getBalance().signum() != 0) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "NON_ZERO_BALANCE",
                    "An account can only be closed when its balance is zero"
            );
        }
        account.close();
        return accountMapper.toResponse(accountRepository.saveAndFlush(account));
    }

    public BankAccountEntity getEntity(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
    }

    public BankAccountEntity getEntityForUpdate(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
    }

    private void ensureNotClosed(BankAccountEntity account) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_CLOSED", "Closed accounts cannot change state");
        }
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "4" + String.format("%015d", RANDOM.nextLong(ACCOUNT_NUMBER_SPACE));
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique account number");
    }
}
