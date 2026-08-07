package com.ticketwave.application;

import com.ticketwave.domain.bus.EventBus;
import com.ticketwave.domain.events.FraudDetected;
import com.ticketwave.domain.user.AppUser;
import com.ticketwave.infrastructure.dto.fraud.FraudReportResponse;
import com.ticketwave.infrastructure.exception.FraudRiskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class FraudService {

    private static final Logger log = LoggerFactory.getLogger(FraudService.class);
    private static final String ATTEMPT_KEY = "fraud:attempts:";
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final EventBus eventBus;

    public FraudService(StringRedisTemplate redisTemplate, EventBus eventBus) {
        this.redisTemplate = redisTemplate;
        this.eventBus = eventBus;
    }

    public FraudReportResponse evaluate(AppUser user, String ipAddress) {
        String key = user != null ? ATTEMPT_KEY + user.getId() : ATTEMPT_KEY + "anon:" + ipAddress;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        String duplicateSignal = user != null
                ? redisTemplate.opsForValue().get("fraud:dup:" + user.getId())
                : null;
        boolean duplicate = duplicateSignal != null;

        boolean blocked = attempts != null && attempts > MAX_ATTEMPTS || duplicate;
        String riskLevel = attempts != null && attempts > MAX_ATTEMPTS ? "HIGH" : "LOW";

        if (blocked) {
            log.warn("Fraud risk detected for user={} ip={} attempts={} duplicate={}", user, ipAddress, attempts, duplicate);
        }
        return new FraudReportResponse(user != null ? user.getId().toString() : null,
                ipAddress, riskLevel, "order_attempt_rate", blocked, duplicate,
                blocked ? "Blocked due to suspicious activity" : "OK");
    }

    public void guard(AppUser user, String ipAddress) {
        FraudReportResponse report = evaluate(user, ipAddress);
        if (report.blocked()) {
            eventBus.publish(new FraudDetected(UUID.randomUUID(), Instant.now(),
                    user != null ? user.getId() : null, ipAddress, report.message()));
            throw new FraudRiskException(report.message());
        }
    }

    public void markOrder(UUID orderId, AppUser user) {
        String key = "fraud:dup:" + user.getId();
        redisTemplate.opsForValue().setIfAbsent(key, orderId.toString(), Duration.ofMinutes(5));
    }
}