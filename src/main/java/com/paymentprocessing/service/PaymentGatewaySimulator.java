package com.paymentprocessing.service;

import com.paymentprocessing.kafka.PaymentEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewaySimulator {
    private final int deterministicFailModulo;

    public PaymentGatewaySimulator(@Value("${app.gateway.deterministic-fail-modulo}") int deterministicFailModulo) {
        this.deterministicFailModulo = deterministicFailModulo;
    }

    public void charge(PaymentEvent event) {
        int bucket = Math.floorMod(event.paymentId().hashCode(), deterministicFailModulo);
        if (bucket == 0) {
            throw new TemporaryPaymentProcessingException("Simulated gateway timeout for payment " + event.paymentId());
        }
    }
}
