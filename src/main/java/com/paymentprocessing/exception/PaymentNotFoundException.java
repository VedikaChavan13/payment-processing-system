package com.paymentprocessing.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends ApiException {
    public PaymentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found");
    }
}
