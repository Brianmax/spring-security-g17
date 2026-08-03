package com.jwt.codigo.entity;

import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.enums.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class TransferEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false, updatable = false)
    private BankAccountEntity sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false, updatable = false)
    private BankAccountEntity destinationAccount;

    @Column(nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "destination_amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal destinationAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_currency", nullable = false, updatable = false, length = 3)
    private CurrencyCode sourceCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_currency", nullable = false, updatable = false, length = 3)
    private CurrencyCode destinationCurrency;

    @Column(name = "exchange_rate", nullable = false, updatable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "exchange_rate_date", updatable = false)
    private LocalDate exchangeRateDate;

    @Column(name = "exchange_rate_provider", nullable = false, updatable = false, length = 40)
    private String exchangeRateProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String reference;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 128)
    private String idempotencyKey;

    @Column(updatable = false, length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    protected TransferEntity() {
    }

    public TransferEntity(
            BankAccountEntity sourceAccount,
            BankAccountEntity destinationAccount,
            BigDecimal amount,
            BigDecimal destinationAmount,
            BigDecimal exchangeRate,
            LocalDate exchangeRateDate,
            String exchangeRateProvider,
            String reference,
            String idempotencyKey,
            String description
    ) {
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.destinationAmount = destinationAmount;
        this.sourceCurrency = sourceAccount.getCurrency();
        this.destinationCurrency = destinationAccount.getCurrency();
        this.exchangeRate = exchangeRate;
        this.exchangeRateDate = exchangeRateDate;
        this.exchangeRateProvider = exchangeRateProvider;
        this.status = TransferStatus.PENDING;
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void complete() {
        status = TransferStatus.COMPLETED;
        completedAt = Instant.now();
        failureReason = null;
    }

    public void fail(String reason) {
        status = TransferStatus.FAILED;
        failureReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public BankAccountEntity getSourceAccount() {
        return sourceAccount;
    }

    public BankAccountEntity getDestinationAccount() {
        return destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getDestinationAmount() {
        return destinationAmount;
    }

    public CurrencyCode getSourceCurrency() {
        return sourceCurrency;
    }

    public CurrencyCode getDestinationCurrency() {
        return destinationCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public LocalDate getExchangeRateDate() {
        return exchangeRateDate;
    }

    public String getExchangeRateProvider() {
        return exchangeRateProvider;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
