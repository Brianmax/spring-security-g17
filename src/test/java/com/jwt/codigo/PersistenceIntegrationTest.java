package com.jwt.codigo;

import com.jwt.codigo.entity.BankAccountEntity;
import com.jwt.codigo.entity.UserEntity;
import com.jwt.codigo.enums.AccountType;
import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.repository.BankAccountRepository;
import com.jwt.codigo.repository.UserRepository;
import com.jwt.codigo.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistenceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository accountRepository;

    @Test
    void liquibaseSchemaSupportsUsersAccountsAndUniqueEmails() {
        UserEntity owner = userRepository.saveAndFlush(
                new UserEntity("Ada", "Lovelace", "ada@example.com")
        );
        BankAccountEntity account = accountRepository.saveAndFlush(
                new BankAccountEntity("1000000000000001", AccountType.CHECKING, CurrencyCode.USD, owner)
        );

        assertThat(accountRepository.findById(account.getId())).isPresent();
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getBalance()).isEqualByComparingTo("0.0000");

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new UserEntity("Another", "Owner", "ada@example.com")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
