package com.paymentprocessing.service;

import com.paymentprocessing.entity.PaymentStatus;
import com.paymentprocessing.exception.InvalidPaymentException;
import com.paymentprocessing.util.PaymentStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {
    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    @Test
    void allowsValidTransitions() {
        assertThatCode(() -> stateMachine.validate(PaymentStatus.PENDING, PaymentStatus.PROCESSING))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validate(PaymentStatus.PROCESSING, PaymentStatus.COMPLETED))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validate(PaymentStatus.PROCESSING, PaymentStatus.FAILED))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidTerminalTransitions() {
        assertThatThrownBy(() -> stateMachine.validate(PaymentStatus.COMPLETED, PaymentStatus.PROCESSING))
                .isInstanceOf(InvalidPaymentException.class);
        assertThatThrownBy(() -> stateMachine.validate(PaymentStatus.FAILED, PaymentStatus.COMPLETED))
                .isInstanceOf(InvalidPaymentException.class);
    }
}
