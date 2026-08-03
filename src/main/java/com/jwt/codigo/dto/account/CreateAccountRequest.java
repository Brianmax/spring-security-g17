package com.jwt.codigo.dto.account;

import com.jwt.codigo.enums.AccountType;
import com.jwt.codigo.enums.CurrencyCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "New virtual bank account; initial balance is always zero")
public record CreateAccountRequest(
        @NotNull @Schema(example = "CHECKING") AccountType accountType,
        @NotNull @Schema(example = "USD", allowableValues = {"USD", "EUR", "PEN"}) CurrencyCode currency
) {
}
