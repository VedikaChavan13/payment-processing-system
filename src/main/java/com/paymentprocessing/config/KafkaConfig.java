package com.paymentprocessing.config;

import com.paymentprocessing.kafka.PaymentDlqEvent;
import com.paymentprocessing.kafka.PaymentEvent;
import com.paymentprocessing.service.PaymentProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Instant;
import java.util.UUID;

@Configuration
public class KafkaConfig {
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentEvent> consumerFactory,
            DefaultErrorHandler paymentErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(paymentErrorHandler);
        return factory;
    }

    @Bean
    DefaultErrorHandler paymentErrorHandler(KafkaTemplate<String, Object> kafkaTemplate,
                                            PaymentProcessingService paymentProcessingService,
                                            @Value("${app.kafka.payment-dlq-topic}") String dlqTopic,
                                            @Value("${app.kafka.max-retries}") int maxRetries,
                                            @Value("${app.kafka.retry-backoff-ms}") long retryBackoffMs) {
        return new DefaultErrorHandler((record, exception) -> {
            Object originalValue = record.value();
            if (originalValue instanceof PaymentEvent event) {
                paymentProcessingService.markFailedAfterRetries(event, exception);
                PaymentDlqEvent dlqEvent = new PaymentDlqEvent(
                        UUID.randomUUID(),
                        event,
                        exception.getMessage(),
                        Instant.now(),
                        maxRetries,
                        event.paymentId());
                kafkaTemplate.send(dlqTopic, event.paymentId().toString(), dlqEvent).join();
            }
        }, new FixedBackOff(retryBackoffMs, maxRetries));
    }
}
