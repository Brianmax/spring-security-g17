package com.jwt.codigo.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Consistent error response returned for validation and business failures")
public record ApiErrorResponse(
        @Schema(example = "2026-08-03T15:30:00Z") Instant timestamp,
        @Schema(example = "422") int status,
        @Schema(example = "INSUFFICIENT_FUNDS") String code,
        @Schema(example = "The account has insufficient funds") String message,
        @Schema(example = "/api/v1/accounts/7c03c03b-a6b8-4bea-a117-21bdcf1ba1e7/withdrawals") String path,
        @Schema(example = "6eef1183-3785-4d64-a11f-0649f5486a91") String requestId,
        @Schema(description = "Field validation messages; empty for non-validation errors") Map<String, String> fieldErrors
) {
}
