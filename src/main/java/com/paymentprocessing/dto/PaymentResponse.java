package com.paymentprocessing.dto;

import com.paymentprocessing.entity.PaymentMethod;
import com.paymentprocessing.entity.PaymentStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
