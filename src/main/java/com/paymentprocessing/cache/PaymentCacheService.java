package com.paymentprocessing.cache;

import com.paymentprocessing.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCacheService {
    private static final Logger log = LoggerFactory.getLogger(PaymentCacheService.class);
    private final RedisTemplate<String, PaymentResponse> redisTemplate;

    @Value("${app.cache.payment-ttl}")
    private Duration ttl;

    public Optional<PaymentResponse> get(UUID paymentId) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key(paymentId)));
        } catch (Exception ex) {
            log.warn("Redis read failed for payment {}", paymentId);
            return Optional.empty();
        }
    }

    public void put(PaymentResponse response) {
        try {
            redisTemplate.opsForValue().set(key(response.paymentId()), response, ttl);
        } catch (Exception ex) {
            log.warn("Redis write failed for payment {}", response.paymentId());
        }
    }

    public void evict(UUID paymentId) {
        try {
            redisTemplate.delete(key(paymentId));
        } catch (Exception ex) {
            log.warn("Redis eviction failed for payment {}", paymentId);
        }
    }

    private String key(UUID paymentId) {
        return "payment:" + paymentId;
    }
}
