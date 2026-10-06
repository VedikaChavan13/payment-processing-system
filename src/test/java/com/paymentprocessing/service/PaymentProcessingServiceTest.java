package com.paymentprocessing.service;

import com.paymentprocessing.cache.PaymentCacheService;
import com.paymentprocessing.entity.Payment;
import com.paymentprocessing.entity.PaymentMethod;
import com.paymentprocessing.entity.PaymentStatus;
import com.paymentprocessing.kafka.PaymentEvent;
import com.paymentprocessing.repository.PaymentRepository;
import com.paymentprocessing.repository.PaymentTransactionRepository;
import com.paymentprocessing.util.PaymentStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentTransactionRepository transactionRepository;
    @Mock
    private PaymentCacheService cacheService;

    @Test
    void completesPendingPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), BigDecimal.TEN, "USD", PaymentMethod.CARD, "idem-1");
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        PaymentGatewaySimulator gateway = new PaymentGatewaySimulator(Integer.MAX_VALUE);
        var service = new PaymentProcessingService(
                paymentRepository, transactionRepository, new PaymentStateMachine(), gateway, cacheService);

        service.process(new PaymentEvent(UUID.randomUUID(), paymentId, payment.getUserId(), BigDecimal.TEN, "USD",
                "PAYMENT_CREATED", Instant.now()));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(any());
        verify(cacheService, org.mockito.Mockito.atLeastOnce()).evict(payment.getId());
    }
}
