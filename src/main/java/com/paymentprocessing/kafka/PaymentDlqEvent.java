package com.paymentprocessing.kafka;

import java.time.Instant;
import java.util.UUID;

public record PaymentDlqEvent(
        UUID dlqEventId,
        PaymentEvent originalEvent,
        String errorMessage,
        Instant timestamp,
        int retryCount,
        UUID paymentId
) {
}
