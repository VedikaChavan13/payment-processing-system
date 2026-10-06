package com.paymentprocessing.kafka;

import com.paymentprocessing.service.PaymentProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentProcessorConsumer {
    private final PaymentProcessingService paymentProcessingService;

    @KafkaListener(topics = "${app.kafka.payment-topic}", containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentEvent(PaymentEvent event) {
        paymentProcessingService.process(event);
    }
}
