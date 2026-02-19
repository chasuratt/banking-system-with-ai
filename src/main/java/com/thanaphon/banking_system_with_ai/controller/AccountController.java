package com.thanaphon.banking_system_with_ai.controller;

import com.thanaphon.banking_system_with_ai.dto.request.CreateAccountRequest;
import com.thanaphon.banking_system_with_ai.dto.response.AccountResponse;
import com.thanaphon.banking_system_with_ai.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account lifecycle operations — create, retrieve, and list accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new bank account",
            description = "Opens a new account with an initial deposit. Minimum deposit is $100.00 (BR-002).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error — invalid owner name or initial deposit below minimum")
    })
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        URI location = URI.create("/api/accounts/" + response.getAccountNumber());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details",
            description = "Returns account number, owner name, balance, and creation date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse getAccount(
            @Parameter(description = "Account number (e.g. A3K9X7M2P1)", required = true)
            @PathVariable("accountId") String accountNumber) {
        return accountService.getAccountByNumber(accountNumber);
    }

    @GetMapping
    @Operation(summary = "List all accounts",
            description = "Returns all accounts in the system. Returns an empty list if none exist.")
    @ApiResponse(responseCode = "200", description = "List of accounts (may be empty)")
    public List<AccountResponse> listAccounts() {
        return accountService.listAccounts();
    }
}
