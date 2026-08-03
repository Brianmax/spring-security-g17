package com.jwt.codigo.dto.transaction;

import com.jwt.codigo.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Immutable account ledger entry")
public record TransactionResponse(
        UUID id,
        UUID accountId,
        TransactionType transactionType,
        @Schema(example = "100.0000") BigDecimal amount,
        @Schema(example = "850.0000") BigDecimal balanceAfterTransaction,
        String reference,
        String description,
        Instant createdAt
) {
}
