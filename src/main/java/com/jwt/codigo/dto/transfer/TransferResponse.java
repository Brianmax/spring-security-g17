package com.jwt.codigo.dto.transfer;

import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.enums.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Virtual-money transfer result")
public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID destinationAccountId,
        @Schema(description = "Amount debited from the source account", example = "50.0000") BigDecimal amount,
        @Schema(description = "Amount credited to the destination account", example = "46.2500") BigDecimal destinationAmount,
        CurrencyCode sourceCurrency,
        CurrencyCode destinationCurrency,
        @Schema(description = "Destination currency units per source currency unit", example = "0.92500000")
        BigDecimal exchangeRate,
        LocalDate exchangeRateDate,
        @Schema(example = "DECOLECTA_SBS_AVERAGE") String exchangeRateProvider,
        TransferStatus status,
        String reference,
        String idempotencyKey,
        String description,
        Instant createdAt,
        Instant completedAt,
        String failureReason
) {
}
