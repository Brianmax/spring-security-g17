package com.jwt.codigo.entity;

import com.jwt.codigo.enums.AccountStatus;
import com.jwt.codigo.enums.AccountType;
import com.jwt.codigo.enums.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccountEntity extends AuditableEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Version
    @Column(nullable = false)
    private long version;

    protected BankAccountEntity() {
    }

    public BankAccountEntity(String accountNumber, AccountType accountType, CurrencyCode currency, UserEntity owner) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = BigDecimal.ZERO.setScale(4);
        this.currency = currency;
        this.status = AccountStatus.ACTIVE;
        this.owner = owner;
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    public void freeze() {
        status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        status = AccountStatus.ACTIVE;
    }

    public void close() {
        status = AccountStatus.CLOSED;
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public long getVersion() {
        return version;
    }
}
