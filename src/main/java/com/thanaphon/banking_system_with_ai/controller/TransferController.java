package com.thanaphon.banking_system_with_ai.controller;

import com.thanaphon.banking_system_with_ai.dto.request.TransferRequest;
import com.thanaphon.banking_system_with_ai.dto.response.TransferResponse;
import com.thanaphon.banking_system_with_ai.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Atomic fund transfers between accounts")
public class TransferController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Transfer funds between accounts",
            description = "Atomically debits the source and credits the destination. " +
                    "Both operations succeed or both roll back (BR-005, BR-006). " +
                    "Source and destination must be different accounts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer successful"),
            @ApiResponse(responseCode = "400", description = "Validation error, insufficient funds, or same-account transfer"),
            @ApiResponse(responseCode = "404", description = "Source or destination account not found")
    })
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount());
    }
}
