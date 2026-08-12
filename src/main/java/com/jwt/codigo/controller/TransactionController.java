package com.jwt.codigo.controller;

import com.jwt.codigo.dto.common.PageResponse;
import com.jwt.codigo.dto.transaction.TransactionResponse;
import com.jwt.codigo.exception.ApiErrorDocumentation;
import com.jwt.codigo.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transactions", description = "Read immutable account ledger entries")
@ApiErrorDocumentation
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/accounts/{accountId}/transactions")
    @PreAuthorize("hasAuthority('transaction:read:any') or (hasAuthority('transaction:read:self') and @bankingAuthorization.ownsAccount(authentication, #accountId))")
    @Operation(summary = "List account transactions",
            description = "Returns a zero-based page ordered newest-first by default; maximum size is 100")
    @ApiResponse(responseCode = "200", description = "Transaction page")
    public PageResponse<TransactionResponse> findByAccount(
            @Parameter(description = "Account UUID") @PathVariable UUID accountId,
            @ParameterObject
            @PageableDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return transactionService.findByAccount(accountId, pageable);
    }

    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("hasAuthority('transaction:read:any') or (hasAuthority('transaction:read:self') and @bankingAuthorization.ownsTransaction(authentication, #transactionId))")
    @Operation(summary = "Get a transaction")
    @ApiResponse(responseCode = "200", description = "Transaction found",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class)))
    public TransactionResponse findById(
            @Parameter(description = "Transaction UUID") @PathVariable UUID transactionId
    ) {
        return transactionService.findById(transactionId);
    }
}
