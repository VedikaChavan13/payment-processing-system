package com.paymentprocessing.service;

import com.paymentprocessing.cache.PaymentCacheService;
import com.paymentprocessing.entity.Payment;
import com.paymentprocessing.entity.PaymentStatus;
import com.paymentprocessing.entity.PaymentTransaction;
import com.paymentprocessing.entity.TransactionType;
import com.paymentprocessing.exception.PaymentNotFoundException;
import com.paymentprocessing.kafka.PaymentEvent;
import com.paymentprocessing.repository.PaymentRepository;
import com.paymentprocessing.repository.PaymentTransactionRepository;
import com.paymentprocessing.util.PaymentStateMachine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {
    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentGatewaySimulator gatewaySimulator;
    private final PaymentCacheService paymentCacheService;

    @Transactional
    public void process(PaymentEvent event) {
        Payment payment = paymentRepository.findByIdForUpdate(event.paymentId())
                .orElseThrow(PaymentNotFoundException::new);
        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Skipping already terminal payment {}", payment.getId());
            return;
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            transition(payment, PaymentStatus.PROCESSING, TransactionType.PAYMENT_PROCESSING, null);
        }

        gatewaySimulator.charge(event);
        transition(payment, PaymentStatus.COMPLETED, TransactionType.PAYMENT_COMPLETED, null);
        paymentCacheService.evict(payment.getId());
    }

    @Transactional
    public void markFailedAfterRetries(PaymentEvent event, Exception exception) {
        Payment payment = paymentRepository.findByIdForUpdate(event.paymentId()).orElse(null);
        if (payment == null || payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            transition(payment, PaymentStatus.PROCESSING, TransactionType.PAYMENT_PROCESSING, null);
        }
        transition(payment, PaymentStatus.FAILED, TransactionType.PAYMENT_FAILED, exception.getMessage());
        paymentCacheService.evict(payment.getId());
    }

    private void transition(Payment payment, PaymentStatus next, TransactionType transactionType, String errorMessage) {
        stateMachine.validate(payment.getStatus(), next);
        payment.setStatus(next);
        transactionRepository.save(new PaymentTransaction(payment.getId(), transactionType, next, errorMessage));
        paymentCacheService.evict(payment.getId());
    }
}
