package com.jwt.codigo.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Deposit or withdrawal request")
public record MoneyOperationRequest(
        @NotNull
        @DecimalMin(value = "0.0001", message = "amount must be greater than zero")
        @Digits(integer = 15, fraction = 4)
        @Schema(example = "100.00")
        BigDecimal amount,
        @Size(max = 255) @Schema(example = "Initial deposit") String description
) {
}
