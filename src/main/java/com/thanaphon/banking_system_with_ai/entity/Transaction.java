package com.thanaphon.banking_system_with_ai.entity;

import com.thanaphon.banking_system_with_ai.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Transaction records are immutable (BR-009, BR-010): no @Setter, no updatedAt field.
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_account_id_created_at", columnList = "account_id, created_at"),
        @Index(name = "idx_transactions_transfer_reference", columnList = "transfer_reference")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_account_id")
    private Long referenceAccountId;

    @Column(name = "transfer_reference", length = 36)
    private String transferReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
