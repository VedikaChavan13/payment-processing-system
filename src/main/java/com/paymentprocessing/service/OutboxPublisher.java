package com.paymentprocessing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessing.entity.OutboxEvent;
import com.paymentprocessing.entity.OutboxStatus;
import com.paymentprocessing.kafka.PaymentEvent;
import com.paymentprocessing.kafka.PaymentEventProducer;
import com.paymentprocessing.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms}")
    @Transactional
    public void publishPendingEvents() {
        var events = outboxEventRepository.findBatchForUpdate(OutboxStatus.PENDING, PageRequest.of(0, batchSize));
        for (OutboxEvent event : events) {
            try {
                PaymentEvent paymentEvent = objectMapper.readValue(event.getPayload(), PaymentEvent.class);
                paymentEventProducer.publish(paymentEvent);
                event.markProcessed();
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {}", event.getId(), ex);
                event.markFailed();
            }
        }
    }
}
