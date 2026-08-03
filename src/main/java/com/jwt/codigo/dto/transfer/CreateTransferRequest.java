package com.jwt.codigo.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Atomic virtual-money transfer")
public record CreateTransferRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        @NotNull
        @DecimalMin(value = "0.0001", message = "amount must be greater than zero")
        @Digits(integer = 15, fraction = 4)
        @Schema(example = "50.00")
        BigDecimal amount,
        @Size(max = 255) @Schema(example = "Example transfer") String description,
        @NotBlank @Size(max = 128) @Schema(example = "payment-2026-08-03-001") String idempotencyKey
) {
}
