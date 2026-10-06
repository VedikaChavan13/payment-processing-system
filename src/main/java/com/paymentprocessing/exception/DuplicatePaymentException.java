package com.paymentprocessing.exception;

import org.springframework.http.HttpStatus;

public class DuplicatePaymentException extends ApiException {
    public DuplicatePaymentException() {
        super(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", "A payment already exists for this idempotency key");
    }
}
