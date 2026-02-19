package com.thanaphon.banking_system_with_ai.service;

import com.thanaphon.banking_system_with_ai.dto.response.AccountResponse;
import com.thanaphon.banking_system_with_ai.dto.response.TransactionResponse;
import com.thanaphon.banking_system_with_ai.dto.response.TransferResponse;
import com.thanaphon.banking_system_with_ai.entity.Account;
import com.thanaphon.banking_system_with_ai.entity.Transaction;
import com.thanaphon.banking_system_with_ai.enums.TransactionType;
import com.thanaphon.banking_system_with_ai.exception.AccountNotFoundException;
import com.thanaphon.banking_system_with_ai.exception.InsufficientFundsException;
import com.thanaphon.banking_system_with_ai.exception.SameAccountTransferException;
import com.thanaphon.banking_system_with_ai.repository.AccountRepository;
import com.thanaphon.banking_system_with_ai.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Deposits funds into an account.
     * Acquires a pessimistic write lock on the account row to prevent concurrent modifications (BR-007).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountResponse deposit(String accountNumber, BigDecimal amount) {
        BigDecimal scaledAmount = scale(amount);

        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        BigDecimal newBalance = account.getBalance().add(scaledAmount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .accountId(account.getId())
                .type(TransactionType.DEPOSIT)
                .amount(scaledAmount)
                .balanceAfter(newBalance)
                .build());

        log.info("Deposited {} to account {}, new balance: {}", scaledAmount, accountNumber, newBalance);
        return toAccountResponse(account);
    }

    /**
     * Withdraws funds from an account.
     * Acquires a pessimistic write lock. Throws InsufficientFundsException if balance < amount (BR-003).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountResponse withdraw(String accountNumber, BigDecimal amount) {
        BigDecimal scaledAmount = scale(amount);

        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (account.getBalance().compareTo(scaledAmount) < 0) {
            throw new InsufficientFundsException();
        }

        BigDecimal newBalance = account.getBalance().subtract(scaledAmount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        transactionRepository.save(Transaction.builder()
                .accountId(account.getId())
                .type(TransactionType.WITHDRAWAL)
                .amount(scaledAmount)
                .balanceAfter(newBalance)
                .build());

        log.info("Withdrew {} from account {}, new balance: {}", scaledAmount, accountNumber, newBalance);
        return toAccountResponse(account);
    }

    /**
     * Atomically transfers funds between two accounts (BR-005, BR-006).
     *
     * Locks are acquired in ascending ID order to prevent deadlocks when two concurrent transfers
     * involve the same pair of accounts in opposite directions (AD-3).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse transfer(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount) {
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new SameAccountTransferException();
        }

        BigDecimal scaledAmount = scale(amount);

        // Load without lock first to obtain IDs for ordering
        Account source = accountRepository.findByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException(sourceAccountNumber));
        Account destination = accountRepository.findByAccountNumber(destinationAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException(destinationAccountNumber));

        // Re-lock in ascending ID order to prevent deadlocks (AD-3)
        if (source.getId() < destination.getId()) {
            source = accountRepository.findByIdWithLock(source.getId()).orElseThrow();
            destination = accountRepository.findByIdWithLock(destination.getId()).orElseThrow();
        } else {
            destination = accountRepository.findByIdWithLock(destination.getId()).orElseThrow();
            source = accountRepository.findByIdWithLock(source.getId()).orElseThrow();
        }

        if (source.getBalance().compareTo(scaledAmount) < 0) {
            throw new InsufficientFundsException();
        }

        String transferRef = UUID.randomUUID().toString();
        BigDecimal sourceNewBalance = source.getBalance().subtract(scaledAmount);
        BigDecimal destinationNewBalance = destination.getBalance().add(scaledAmount);

        source.setBalance(sourceNewBalance);
        destination.setBalance(destinationNewBalance);
        accountRepository.save(source);
        accountRepository.save(destination);

        Long sourceId = source.getId();
        Long destinationId = destination.getId();

        transactionRepository.save(Transaction.builder()
                .accountId(sourceId)
                .type(TransactionType.TRANSFER_OUT)
                .amount(scaledAmount)
                .balanceAfter(sourceNewBalance)
                .referenceAccountId(destinationId)
                .transferReference(transferRef)
                .build());

        transactionRepository.save(Transaction.builder()
                .accountId(destinationId)
                .type(TransactionType.TRANSFER_IN)
                .amount(scaledAmount)
                .balanceAfter(destinationNewBalance)
                .referenceAccountId(sourceId)
                .transferReference(transferRef)
                .build());

        log.info("Transfer {} from {} to {}, amount: {}", transferRef, sourceAccountNumber, destinationAccountNumber, scaledAmount);

        return TransferResponse.builder()
                .transferReference(transferRef)
                .sourceAccountNumber(source.getAccountNumber())
                .destinationAccountNumber(destination.getAccountNumber())
                .amount(scaledAmount)
                .sourceBalanceAfter(sourceNewBalance)
                .destinationBalanceAfter(destinationNewBalance)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Returns transaction history for an account, newest first (FR-007).
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(tx -> {
                    String refAccountNumber = null;
                    if (tx.getReferenceAccountId() != null) {
                        refAccountNumber = accountRepository.findById(tx.getReferenceAccountId())
                                .map(Account::getAccountNumber)
                                .orElse(null);
                    }
                    return TransactionResponse.builder()
                            .id(tx.getId())
                            .type(tx.getType())
                            .amount(tx.getAmount())
                            .balanceAfter(tx.getBalanceAfter())
                            .referenceAccountNumber(refAccountNumber)
                            .timestamp(tx.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .ownerName(account.getOwnerName())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
