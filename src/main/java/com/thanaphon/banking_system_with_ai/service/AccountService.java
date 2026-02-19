package com.thanaphon.banking_system_with_ai.service;

import com.thanaphon.banking_system_with_ai.dto.request.CreateAccountRequest;
import com.thanaphon.banking_system_with_ai.dto.response.AccountResponse;
import com.thanaphon.banking_system_with_ai.entity.Account;
import com.thanaphon.banking_system_with_ai.entity.Transaction;
import com.thanaphon.banking_system_with_ai.enums.TransactionType;
import com.thanaphon.banking_system_with_ai.exception.AccountNotFoundException;
import com.thanaphon.banking_system_with_ai.repository.AccountRepository;
import com.thanaphon.banking_system_with_ai.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Creates a new bank account and records the initial deposit as a DEPOSIT transaction (AD-6).
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String accountNumber = generateAccountNumber();
        BigDecimal initialBalance = request.getInitialDeposit().setScale(2, RoundingMode.UNNECESSARY);

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .ownerName(request.getOwnerName())
                .balance(initialBalance)
                .build();
        account = accountRepository.save(account);

        // Record the opening deposit for a complete audit trail (BR-009)
        Transaction initialDeposit = Transaction.builder()
                .accountId(account.getId())
                .type(TransactionType.DEPOSIT)
                .amount(initialBalance)
                .balanceAfter(initialBalance)
                .build();
        transactionRepository.save(initialDeposit);

        log.info("Created account {} for owner '{}'", accountNumber, request.getOwnerName());
        return toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts() {
        return accountRepository.findAll().stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList());
    }

    // Generates a 10-character alphanumeric account number from a UUID (NFR-005, NFR-011).
    private String generateAccountNumber() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 10);
    }

    AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .ownerName(account.getOwnerName())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }

    public Account getAccountEntityByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    BigDecimal scaleAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
