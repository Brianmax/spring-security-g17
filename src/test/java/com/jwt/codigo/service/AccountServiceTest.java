package com.jwt.codigo.service;

import com.jwt.codigo.dto.account.AccountResponse;
import com.jwt.codigo.dto.account.CreateAccountRequest;
import com.jwt.codigo.entity.BankAccountEntity;
import com.jwt.codigo.entity.UserEntity;
import com.jwt.codigo.enums.AccountStatus;
import com.jwt.codigo.enums.AccountType;
import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.enums.UserStatus;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.AccountMapper;
import com.jwt.codigo.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private UserService userService;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, userService, new AccountMapper());
    }

    @Test
    void createsAccountForActiveUser() {
        UserEntity owner = new UserEntity("Ada", "Lovelace", "ada@example.com");
        when(userService.getEntity(owner.getId())).thenReturn(owner);
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.saveAndFlush(any(BankAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.create(
                owner.getId(),
                new CreateAccountRequest(AccountType.CHECKING, CurrencyCode.USD)
        );

        assertThat(response.ownerId()).isEqualTo(owner.getId());
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void rejectsAccountForInactiveUser() {
        UserEntity owner = new UserEntity("Ada", "Lovelace", "ada@example.com");
        owner.update("Ada", "Lovelace", "ada@example.com", UserStatus.INACTIVE);
        when(userService.getEntity(owner.getId())).thenReturn(owner);

        assertThatThrownBy(() -> accountService.create(
                owner.getId(),
                new CreateAccountRequest(AccountType.CHECKING, CurrencyCode.USD)
        )).isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("USER_INACTIVE"));
    }

    @Test
    void rejectsClosingAccountWithBalance() {
        UserEntity owner = new UserEntity("Ada", "Lovelace", "ada@example.com");
        BankAccountEntity account = new BankAccountEntity(
                "4000000000000001", AccountType.SAVINGS, CurrencyCode.USD, owner
        );
        account.credit(new BigDecimal("1.0000"));
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.close(account.getId()))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("NON_ZERO_BALANCE"));
    }
}
