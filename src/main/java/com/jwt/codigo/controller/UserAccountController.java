package com.jwt.codigo.controller;

import com.jwt.codigo.dto.account.AccountResponse;
import com.jwt.codigo.dto.account.CreateAccountRequest;
import com.jwt.codigo.dto.common.PageResponse;
import com.jwt.codigo.exception.ApiErrorDocumentation;
import com.jwt.codigo.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/accounts")
@Tag(name = "Accounts", description = "Open and list a user's virtual bank accounts")
@ApiErrorDocumentation
public class UserAccountController {

    private final AccountService accountService;

    public UserAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Open an account", description = "Inactive users cannot open accounts; initial balance is zero")
    @ApiResponse(responseCode = "201", description = "Account created",
            content = @Content(schema = @Schema(implementation = AccountResponse.class)))
    public ResponseEntity<AccountResponse> create(
            @Parameter(description = "Owner UUID") @PathVariable UUID userId,
            @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountResponse response = accountService.create(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "List a user's accounts",
            description = "Returns a zero-based page; default size is 20 and maximum size is 100")
    @ApiResponse(responseCode = "200", description = "Account page")
    public PageResponse<AccountResponse> findByOwner(
            @Parameter(description = "Owner UUID") @PathVariable UUID userId,
            @ParameterObject @PageableDefault(sort = "createdAt") Pageable pageable
    ) {
        return accountService.findByOwner(userId, pageable);
    }
}
