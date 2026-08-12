package com.jwt.codigo.controller;

import com.jwt.codigo.dto.transfer.CreateTransferRequest;
import com.jwt.codigo.dto.transfer.TransferResponse;
import com.jwt.codigo.exception.ApiErrorDocumentation;
import com.jwt.codigo.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Atomically move virtual money between accounts")
@ApiErrorDocumentation
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('transfer:create:any') or (hasAuthority('transfer:create:self') and @bankingAuthorization.ownsAccount(authentication, #request.sourceAccountId()))")
    @Operation(summary = "Transfer virtual money",
            description = "Transfers in the same currency use a 1:1 rate. Cross-currency transfers use Decolecta SBS "
                    + "average buy/sell rates. Returns 201 for a new transfer and 200 for an idempotent replay.")
    @ApiResponse(responseCode = "201", description = "Transfer completed",
            content = @Content(schema = @Schema(implementation = TransferResponse.class)))
    @ApiResponse(responseCode = "200", description = "Existing transfer returned for an idempotent replay",
            content = @Content(schema = @Schema(implementation = TransferResponse.class)))
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody CreateTransferRequest request) {
        TransferService.TransferResult result = transferService.transfer(request);
        if (!result.created()) {
            return ResponseEntity.ok(result.response());
        }
        return ResponseEntity
                .created(URI.create("/api/v1/transfers/" + result.response().id()))
                .body(result.response());
    }

    @GetMapping("/{transferId}")
    @PreAuthorize("hasAuthority('transfer:read:any') or (hasAuthority('transfer:read:self') and @bankingAuthorization.participatesInTransfer(authentication, #transferId))")
    @Operation(summary = "Get a transfer")
    @ApiResponse(responseCode = "200", description = "Transfer found",
            content = @Content(schema = @Schema(implementation = TransferResponse.class)))
    public TransferResponse findById(
            @Parameter(description = "Transfer UUID") @PathVariable UUID transferId
    ) {
        return transferService.findById(transferId);
    }
}
