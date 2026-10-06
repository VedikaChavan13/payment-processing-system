package com.paymentprocessing.controller;

import com.paymentprocessing.dto.CreatePaymentRequest;
import com.paymentprocessing.dto.PaymentResponse;
import com.paymentprocessing.entity.PaymentStatus;
import com.paymentprocessing.exception.InvalidPaymentException;
import com.paymentprocessing.security.UserPrincipal;
import com.paymentprocessing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestHeader("Idempotency-Key") String idempotencyKey,
                                         @Valid @RequestBody CreatePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 255) {
            throw new InvalidPaymentException("Idempotency-Key header is required and must be at most 255 characters");
        }
        return paymentService.createPayment(principal.id(), request, idempotencyKey);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable UUID paymentId) {
        return paymentService.getPayment(principal.id(), paymentId);
    }

    @GetMapping
    public Page<PaymentResponse> listPayments(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestParam(required = false) PaymentStatus status,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return paymentService.listPayments(principal.id(), status, pageable);
    }
}
