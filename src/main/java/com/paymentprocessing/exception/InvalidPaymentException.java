package com.paymentprocessing.exception;

import org.springframework.http.HttpStatus;

public class InvalidPaymentException extends ApiException {
    public InvalidPaymentException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT", message);
    }
}
