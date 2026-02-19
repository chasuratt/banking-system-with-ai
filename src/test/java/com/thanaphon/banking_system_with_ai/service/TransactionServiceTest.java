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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        accountA = Account.builder()
                .id(1L).accountNumber("ACCT000001").ownerName("Alice")
                .balance(new BigDecimal("1000.00")).createdAt(LocalDateTime.now()).build();

        accountB = Account.builder()
                .id(2L).accountNumber("ACCT000002").ownerName("Bob")
                .balance(new BigDecimal("500.00")).createdAt(LocalDateTime.now()).build();
    }

    // ── deposit ────────────────────────────────────────────────────────────────

    @Test
    void deposit_happyPath_increasesBalance() {
        when(accountRepository.findByAccountNumberWithLock("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.save(any(Account.class))).thenReturn(accountA);

        AccountResponse response = transactionService.deposit("ACCT000001", new BigDecimal("250.00"));

        assertThat(response.getBalance()).isEqualByComparingTo("1250.00");
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void deposit_recordsDepositTransaction() {
        when(accountRepository.findByAccountNumberWithLock("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.save(any(Account.class))).thenReturn(accountA);

        transactionService.deposit("ACCT000001", new BigDecimal("100.00"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void deposit_accountNotFound_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumberWithLock("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deposit("UNKNOWN", new BigDecimal("100.00")))
                .isInstanceOf(AccountNotFoundException.class);
        verify(transactionRepository, never()).save(any());
    }

    // ── withdraw ───────────────────────────────────────────────────────────────

    @Test
    void withdraw_happyPath_decreasesBalance() {
        when(accountRepository.findByAccountNumberWithLock("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.save(any(Account.class))).thenReturn(accountA);

        AccountResponse response = transactionService.withdraw("ACCT000001", new BigDecimal("300.00"));

        assertThat(response.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void withdraw_exactBalance_succeeds() {
        when(accountRepository.findByAccountNumberWithLock("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.save(any(Account.class))).thenReturn(accountA);

        AccountResponse response = transactionService.withdraw("ACCT000001", new BigDecimal("1000.00"));

        assertThat(response.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void withdraw_insufficientFunds_throwsInsufficientFundsException() {
        when(accountRepository.findByAccountNumberWithLock("ACCT000001")).thenReturn(Optional.of(accountA));

        assertThatThrownBy(() -> transactionService.withdraw("ACCT000001", new BigDecimal("1000.01")))
                .isInstanceOf(InsufficientFundsException.class);
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void withdraw_accountNotFound_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumberWithLock("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.withdraw("UNKNOWN", new BigDecimal("100.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void withdraw_recordsWithdrawalTransaction() {
        when(accountRepository.findByAccountNumberWithLock("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.save(any(Account.class))).thenReturn(accountA);

        transactionService.withdraw("ACCT000001", new BigDecimal("200.00"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    // ── transfer ───────────────────────────────────────────────────────────────

    @Test
    void transfer_happyPath_debitsSourceAndCreditsDestination() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumber("ACCT000002")).thenReturn(Optional.of(accountB));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(accountB));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferResponse response = transactionService.transfer("ACCT000001", "ACCT000002", new BigDecimal("200.00"));

        assertThat(response.getSourceBalanceAfter()).isEqualByComparingTo("800.00");
        assertThat(response.getDestinationBalanceAfter()).isEqualByComparingTo("700.00");
        assertThat(response.getTransferReference()).isNotBlank();
    }

    @Test
    void transfer_createsTwoTransactionRecords() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumber("ACCT000002")).thenReturn(Optional.of(accountB));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(accountB));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.transfer("ACCT000001", "ACCT000002", new BigDecimal("100.00"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();
        assertThat(saved).extracting(Transaction::getType)
                .containsExactlyInAnyOrder(TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN);
    }

    @Test
    void transfer_bothTransactionsSameReference() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumber("ACCT000002")).thenReturn(Optional.of(accountB));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(accountB));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.transfer("ACCT000001", "ACCT000002", new BigDecimal("50.00"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<String> refs = captor.getAllValues().stream()
                .map(Transaction::getTransferReference).toList();
        assertThat(refs.get(0)).isEqualTo(refs.get(1));
    }

    @Test
    void transfer_sameAccount_throwsSameAccountTransferException() {
        assertThatThrownBy(() -> transactionService.transfer("ACCT000001", "ACCT000001", new BigDecimal("100.00")))
                .isInstanceOf(SameAccountTransferException.class);
        verify(accountRepository, never()).findByAccountNumber(any());
    }

    @Test
    void transfer_insufficientFunds_throwsInsufficientFundsException() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumber("ACCT000002")).thenReturn(Optional.of(accountB));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(accountB));

        assertThatThrownBy(() -> transactionService.transfer("ACCT000001", "ACCT000002", new BigDecimal("1000.01")))
                .isInstanceOf(InsufficientFundsException.class);
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transfer_sourceNotFound_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer("UNKNOWN", "ACCT000002", new BigDecimal("100.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void transfer_destinationNotFound_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer("ACCT000001", "UNKNOWN", new BigDecimal("100.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void transfer_exactSourceBalance_succeeds() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumber("ACCT000002")).thenReturn(Optional.of(accountB));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(accountA));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(accountB));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferResponse response = transactionService.transfer("ACCT000001", "ACCT000002", new BigDecimal("1000.00"));

        assertThat(response.getSourceBalanceAfter()).isEqualByComparingTo("0.00");
    }

    // ── getTransactionHistory ─────────────────────────────────────────────────

    @Test
    void getTransactionHistory_returnsTransactionsNewestFirst() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        LocalDateTime t1 = LocalDateTime.now().minusHours(2);
        LocalDateTime t2 = LocalDateTime.now().minusHours(1);
        Transaction tx1 = Transaction.builder().id(1L).accountId(1L)
                .type(TransactionType.DEPOSIT).amount(new BigDecimal("200.00"))
                .balanceAfter(new BigDecimal("200.00")).createdAt(t1).build();
        Transaction tx2 = Transaction.builder().id(2L).accountId(1L)
                .type(TransactionType.WITHDRAWAL).amount(new BigDecimal("50.00"))
                .balanceAfter(new BigDecimal("150.00")).createdAt(t2).build();
        // Repository returns newest first
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(tx2, tx1));

        List<TransactionResponse> history = transactionService.getTransactionHistory("ACCT000001");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(history.get(1).getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void getTransactionHistory_accountNotFound_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionHistory("UNKNOWN"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getTransactionHistory_noTransactions_returnsEmptyList() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        List<TransactionResponse> history = transactionService.getTransactionHistory("ACCT000001");

        assertThat(history).isEmpty();
    }

    @Test
    void getTransactionHistory_transferTransaction_populatesReferenceAccountNumber() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(accountA));
        Transaction transferOut = Transaction.builder().id(3L).accountId(1L)
                .type(TransactionType.TRANSFER_OUT).amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("900.00"))
                .referenceAccountId(2L).transferReference("ref-uuid")
                .createdAt(LocalDateTime.now()).build();
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(transferOut));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(accountB));

        List<TransactionResponse> history = transactionService.getTransactionHistory("ACCT000001");

        assertThat(history.get(0).getReferenceAccountNumber()).isEqualTo("ACCT000002");
    }
}
