package com.thanaphon.banking_system_with_ai.controller;

import com.thanaphon.banking_system_with_ai.dto.request.DepositRequest;
import com.thanaphon.banking_system_with_ai.dto.request.WithdrawRequest;
import com.thanaphon.banking_system_with_ai.dto.response.AccountResponse;
import com.thanaphon.banking_system_with_ai.dto.response.TransactionResponse;
import com.thanaphon.banking_system_with_ai.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Deposit, withdrawal, and transaction history operations")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/{accountId}/deposits")
    @Operation(summary = "Deposit funds",
            description = "Increases account balance by the specified amount. Amount must be > $0 and <= $1,000,000.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit successful — returns updated account details"),
            @ApiResponse(responseCode = "400", description = "Validation error — invalid amount"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse deposit(
            @Parameter(description = "Account number", required = true)
            @PathVariable("accountId") String accountNumber,
            @Valid @RequestBody DepositRequest request) {
        return transactionService.deposit(accountNumber, request.getAmount());
    }

    @PostMapping("/{accountId}/withdrawals")
    @Operation(summary = "Withdraw funds",
            description = "Decreases account balance. Fails if balance is insufficient (BR-003).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal successful — returns updated account details"),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse withdraw(
            @Parameter(description = "Account number", required = true)
            @PathVariable("accountId") String accountNumber,
            @Valid @RequestBody WithdrawRequest request) {
        return transactionService.withdraw(accountNumber, request.getAmount());
    }

    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Get transaction history",
            description = "Returns all transactions for the account ordered by date descending (newest first).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history (may be empty)"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public List<TransactionResponse> getTransactionHistory(
            @Parameter(description = "Account number", required = true)
            @PathVariable("accountId") String accountNumber) {
        return transactionService.getTransactionHistory(accountNumber);
    }
}
