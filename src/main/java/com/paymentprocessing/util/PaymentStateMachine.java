package com.paymentprocessing.util;

import com.paymentprocessing.entity.PaymentStatus;
import com.paymentprocessing.exception.InvalidPaymentException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateMachine {
    private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS = Map.of(
            PaymentStatus.PENDING, Set.of(PaymentStatus.PROCESSING),
            PaymentStatus.PROCESSING, Set.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED),
            PaymentStatus.COMPLETED, Set.of(),
            PaymentStatus.FAILED, Set.of()
    );

    public void validate(PaymentStatus current, PaymentStatus next) {
        if (!VALID_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new InvalidPaymentException("Invalid payment transition from " + current + " to " + next);
        }
    }
}
