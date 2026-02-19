package com.thanaphon.banking_system_with_ai.service;

import com.thanaphon.banking_system_with_ai.dto.request.CreateAccountRequest;
import com.thanaphon.banking_system_with_ai.dto.response.AccountResponse;
import com.thanaphon.banking_system_with_ai.entity.Account;
import com.thanaphon.banking_system_with_ai.entity.Transaction;
import com.thanaphon.banking_system_with_ai.enums.TransactionType;
import com.thanaphon.banking_system_with_ai.exception.AccountNotFoundException;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("ACCT000001")
                .ownerName("Alice")
                .balance(new BigDecimal("500.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── createAccount ──────────────────────────────────────────────────────────

    @Test
    void createAccount_happyPath_returnsAccountResponse() {
        CreateAccountRequest request = new CreateAccountRequest("Alice", new BigDecimal("500.00"));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.getOwnerName()).isEqualTo("Alice");
        assertThat(response.getBalance()).isEqualByComparingTo("500.00");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void createAccount_recordsInitialDepositTransaction() {
        CreateAccountRequest request = new CreateAccountRequest("Bob", new BigDecimal("200.00"));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        accountService.createAccount(request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction tx = captor.getValue();
        assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.getAmount()).isEqualByComparingTo("200.00"); // from the request initial deposit
    }

    @Test
    void createAccount_minimumDepositExactly100_succeeds() {
        CreateAccountRequest request = new CreateAccountRequest("Carol", new BigDecimal("100.00"));
        Account minAccount = Account.builder()
                .id(2L).accountNumber("ACCT000002").ownerName("Carol")
                .balance(new BigDecimal("100.00")).createdAt(LocalDateTime.now()).build();
        when(accountRepository.save(any(Account.class))).thenReturn(minAccount);

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.getBalance()).isEqualByComparingTo("100.00");
    }

    // ── getAccountByNumber ─────────────────────────────────────────────────────

    @Test
    void getAccountByNumber_existingAccount_returnsResponse() {
        when(accountRepository.findByAccountNumber("ACCT000001")).thenReturn(Optional.of(sampleAccount));

        AccountResponse response = accountService.getAccountByNumber("ACCT000001");

        assertThat(response.getAccountNumber()).isEqualTo("ACCT000001");
        assertThat(response.getOwnerName()).isEqualTo("Alice");
        assertThat(response.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void getAccountByNumber_nonExistentAccount_throwsAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByNumber("UNKNOWN"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // ── listAccounts ───────────────────────────────────────────────────────────

    @Test
    void listAccounts_returnsAllAccounts() {
        Account second = Account.builder()
                .id(2L).accountNumber("ACCT000002").ownerName("Bob")
                .balance(new BigDecimal("300.00")).createdAt(LocalDateTime.now()).build();
        when(accountRepository.findAll()).thenReturn(List.of(sampleAccount, second));

        List<AccountResponse> result = accountService.listAccounts();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AccountResponse::getAccountNumber)
                .containsExactlyInAnyOrder("ACCT000001", "ACCT000002");
    }

    @Test
    void listAccounts_noAccounts_returnsEmptyList() {
        when(accountRepository.findAll()).thenReturn(List.of());

        List<AccountResponse> result = accountService.listAccounts();

        assertThat(result).isEmpty();
    }
}
