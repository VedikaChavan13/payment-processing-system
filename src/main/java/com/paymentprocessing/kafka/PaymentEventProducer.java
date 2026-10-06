package com.paymentprocessing.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.payment-topic}")
    private String paymentTopic;

    public void publish(PaymentEvent event) {
        kafkaTemplate.send(paymentTopic, event.paymentId().toString(), event).join();
    }
}
