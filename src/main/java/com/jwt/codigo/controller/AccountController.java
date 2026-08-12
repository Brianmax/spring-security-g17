package com.jwt.codigo.controller;

import com.jwt.codigo.dto.account.AccountResponse;
import com.jwt.codigo.exception.ApiErrorDocumentation;
import com.jwt.codigo.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
@Tag(name = "Accounts", description = "Read and change virtual bank account state")
@ApiErrorDocumentation
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('account:read:any') or (hasAuthority('account:read:self') and @bankingAuthorization.ownsAccount(authentication, #accountId))")
    @Operation(summary = "Get an account")
    @ApiResponse(responseCode = "200", description = "Account found",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    public AccountResponse findById(@Parameter(description = "Account UUID") @PathVariable UUID accountId) {
        return accountService.findById(accountId);
    }

    @PatchMapping("/freeze")
    @PreAuthorize("hasAuthority('account:freeze:any')")
    @Operation(summary = "Freeze an account", description = "A frozen account cannot participate in financial operations")
    @ApiResponse(responseCode = "200", description = "Account frozen",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    public AccountResponse freeze(@Parameter(description = "Account UUID") @PathVariable UUID accountId) {
        return accountService.freeze(accountId);
    }

    @PatchMapping("/unfreeze")
    @PreAuthorize("hasAuthority('account:unfreeze:any')")
    @Operation(summary = "Unfreeze an account", description = "Closed accounts cannot be reopened")
    @ApiResponse(responseCode = "200", description = "Account active",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    public AccountResponse unfreeze(@Parameter(description = "Account UUID") @PathVariable UUID accountId) {
        return accountService.unfreeze(accountId);
    }

    @PatchMapping("/close")
    @PreAuthorize("hasAuthority('account:close:any') or (hasAuthority('account:close:self') and @bankingAuthorization.ownsAccount(authentication, #accountId))")
    @Operation(summary = "Close an account", description = "Only an account with zero balance can be closed")
    @ApiResponse(responseCode = "200", description = "Account closed",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    public AccountResponse close(@Parameter(description = "Account UUID") @PathVariable UUID accountId) {
        return accountService.close(accountId);
    }
}
