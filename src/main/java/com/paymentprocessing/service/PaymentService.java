package com.paymentprocessing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessing.cache.PaymentCacheService;
import com.paymentprocessing.dto.CreatePaymentRequest;
import com.paymentprocessing.dto.PaymentResponse;
import com.paymentprocessing.entity.OutboxEvent;
import com.paymentprocessing.entity.Payment;
import com.paymentprocessing.entity.PaymentStatus;
import com.paymentprocessing.entity.PaymentTransaction;
import com.paymentprocessing.entity.TransactionType;
import com.paymentprocessing.exception.PaymentNotFoundException;
import com.paymentprocessing.kafka.PaymentEvent;
import com.paymentprocessing.mapper.PaymentMapper;
import com.paymentprocessing.repository.OutboxEventRepository;
import com.paymentprocessing.repository.PaymentRepository;
import com.paymentprocessing.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentCacheService paymentCacheService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PaymentResponse createPayment(UUID userId, CreatePaymentRequest request, String idempotencyKey) {
        validateCurrency(request.currency());
        try {
            return transactionTemplate.execute(status -> {
                paymentRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                        .ifPresent(existing -> {
                            throw new ExistingPaymentSignal(existing);
                        });
                Payment payment = paymentRepository.saveAndFlush(new Payment(
                        userId,
                        request.amount(),
                        request.currency().toUpperCase(Locale.ROOT),
                        request.paymentMethod(),
                        idempotencyKey));
                transactionRepository.save(new PaymentTransaction(
                        payment.getId(), TransactionType.PAYMENT_CREATED, PaymentStatus.PENDING, null));
                outboxEventRepository.save(new OutboxEvent(
                        payment.getId(), "PAYMENT_CREATED", serialize(toEvent(payment))));
                return paymentMapper.toResponse(payment);
            });
        } catch (ExistingPaymentSignal signal) {
            return paymentMapper.toResponse(signal.payment);
        } catch (DataIntegrityViolationException ex) {
            return paymentRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .map(paymentMapper::toResponse)
                    .orElseThrow(() -> ex);
        }
    }

    public PaymentResponse getPayment(UUID userId, UUID paymentId) {
        return paymentCacheService.get(paymentId)
                .filter(response -> paymentRepository.findByIdAndUserId(paymentId, userId).isPresent())
                .orElseGet(() -> {
                    Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                            .orElseThrow(PaymentNotFoundException::new);
                    PaymentResponse response = paymentMapper.toResponse(payment);
                    paymentCacheService.put(response);
                    return response;
                });
    }

    public Page<PaymentResponse> listPayments(UUID userId, PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = status == null
                ? paymentRepository.findByUserId(userId, pageable)
                : paymentRepository.findByUserIdAndStatus(userId, status, pageable);
        return payments.map(paymentMapper::toResponse);
    }

    private PaymentEvent toEvent(Payment payment) {
        return new PaymentEvent(
                UUID.randomUUID(),
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                "PAYMENT_CREATED",
                Instant.now());
    }

    private String serialize(PaymentEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize payment event", ex);
        }
    }

    private void validateCurrency(String currency) {
        try {
            Currency.getInstance(currency.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new com.paymentprocessing.exception.InvalidPaymentException("Unsupported currency: " + currency);
        }
    }

    private static class ExistingPaymentSignal extends RuntimeException {
        private final Payment payment;

        private ExistingPaymentSignal(Payment payment) {
            super(null, null, false, false);
            this.payment = payment;
        }
    }
}
