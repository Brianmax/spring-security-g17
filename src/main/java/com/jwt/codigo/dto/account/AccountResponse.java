package com.jwt.codigo.dto.account;

import com.jwt.codigo.enums.AccountStatus;
import com.jwt.codigo.enums.AccountType;
import com.jwt.codigo.enums.CurrencyCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Virtual bank account")
public record AccountResponse(
        UUID id,
        @Schema(example = "4123456789012345") String accountNumber,
        AccountType accountType,
        @Schema(example = "1250.0000") BigDecimal balance,
        CurrencyCode currency,
        AccountStatus status,
        UUID ownerId,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
