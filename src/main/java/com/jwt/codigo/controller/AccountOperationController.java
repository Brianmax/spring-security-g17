package com.jwt.codigo.controller;

import com.jwt.codigo.dto.transaction.MoneyOperationRequest;
import com.jwt.codigo.dto.transaction.TransactionResponse;
import com.jwt.codigo.exception.ApiErrorDocumentation;
import com.jwt.codigo.service.AccountOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
@Tag(name = "Account operations", description = "Deposit and withdraw virtual money")
@ApiErrorDocumentation
public class AccountOperationController {

    private final AccountOperationService operationService;

    public AccountOperationController(AccountOperationService operationService) {
        this.operationService = operationService;
    }

    @PostMapping("/deposits")
    @PreAuthorize("hasAuthority('deposit:create:any') or (hasAuthority('deposit:create:self') and @bankingAuthorization.ownsAccount(authentication, #accountId))")
    @Operation(summary = "Deposit virtual money", description = "Atomically credits an active account and creates a DEPOSIT ledger entry")
    @ApiResponse(responseCode = "201", description = "Deposit completed",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    public ResponseEntity<TransactionResponse> deposit(
            @Parameter(description = "Account UUID") @PathVariable UUID accountId,
            @Valid @RequestBody MoneyOperationRequest request
    ) {
        TransactionResponse response = operationService.deposit(accountId, request);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + response.id())).body(response);
    }

    @PostMapping("/withdrawals")
    @PreAuthorize("hasAuthority('withdrawal:create:any') or (hasAuthority('withdrawal:create:self') and @bankingAuthorization.ownsAccount(authentication, #accountId))")
    @Operation(summary = "Withdraw virtual money",
            description = "Atomically debits an active account without overdraft and creates a WITHDRAWAL ledger entry")
    @ApiResponse(responseCode = "201", description = "Withdrawal completed",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(description = "Account UUID") @PathVariable UUID accountId,
            @Valid @RequestBody MoneyOperationRequest request
    ) {
        TransactionResponse response = operationService.withdraw(accountId, request);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + response.id())).body(response);
    }
}
