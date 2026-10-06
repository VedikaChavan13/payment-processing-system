package com.paymentprocessing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payment_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    public PaymentTransaction(UUID paymentId, TransactionType transactionType, PaymentStatus status, String errorMessage) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.transactionType = transactionType;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
