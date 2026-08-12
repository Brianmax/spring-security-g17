package com.jwt.codigo.security;

import com.jwt.codigo.repository.AccountTransactionRepository;
import com.jwt.codigo.repository.BankAccountRepository;
import com.jwt.codigo.repository.TransferRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("bankingAuthorization")
public class BankingAuthorization {
    private final BankAccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final AccountTransactionRepository transactionRepository;

    public BankingAuthorization(BankAccountRepository accountRepository, TransferRepository transferRepository,
                                AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
    }

    public boolean isSelf(Authentication authentication, UUID userId) {
        return currentUser(authentication).equals(userId);
    }

    public boolean ownsAccount(Authentication authentication, UUID accountId) {
        return accountRepository.existsByIdAndOwnerId(accountId, currentUser(authentication));
    }

    public boolean participatesInTransfer(Authentication authentication, UUID transferId) {
        UUID userId = currentUser(authentication);
        return transferRepository.existsByIdAndSourceAccountOwnerIdOrIdAndDestinationAccountOwnerId(
                transferId, userId, transferId, userId);
    }

    public boolean ownsTransaction(Authentication authentication, UUID transactionId) {
        return transactionRepository.existsByIdAndAccountOwnerId(transactionId, currentUser(authentication));
    }

    private UUID currentUser(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (RuntimeException exception) {
            return new UUID(0, 0);
        }
    }
}
