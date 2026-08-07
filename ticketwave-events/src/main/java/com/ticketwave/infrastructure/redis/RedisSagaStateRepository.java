package com.ticketwave.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketwave.domain.saga.SagaState;
import com.ticketwave.domain.saga.SagaStateRepository;
import com.ticketwave.domain.saga.SagaStatus;
import com.ticketwave.domain.saga.SagaStep;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Redis-backed SagaStateRepository. Snapshots are stored in a hash per saga,
 * with a short TTL applied to terminal states (completed / compensated /
 * failed) so the store is cleaned up automatically, while active sagas keep a
 * longer TTL so an interrupted orchestrator can be resumed. An order-id index
 * allows lookups by order without scanning.
 */
@Component
@Profile("!test")
public class RedisSagaStateRepository implements SagaStateRepository {

    private static final String STATE_KEY_PREFIX = "saga:state:";
    private static final String ORDER_INDEX_PREFIX = "saga:order:";
    private static final Duration TERMINAL_TTL = Duration.ofHours(24);
    private static final Duration ACTIVE_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisSagaStateRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(SagaState state) {
        Map<String, String> fields = new HashMap<>();
        fields.put("sagaId", state.sagaId().toString());
        fields.put("orderId", state.orderId().toString());
        fields.put("userId", nullSafe(state.userId()));
        fields.put("eventId", nullSafe(state.eventId()));
        fields.put("quantity", String.valueOf(state.quantity()));
        fields.put("total", state.total().toPlainString());
        fields.put("status", state.status().name());
        fields.put("currentStep", state.currentStep().name());
        fields.put("error", nullSafe(state.error()));
        fields.put("payload", toJson(state.payload()));
        fields.put("createdAt", String.valueOf(state.createdAt().toEpochMilli()));
        fields.put("updatedAt", String.valueOf(state.updatedAt().toEpochMilli()));

        Duration ttl = ttlFor(state.status());
        redis.opsForHash().putAll(stateKey(state.sagaId()), fields);
        redis.expire(stateKey(state.sagaId()), ttl);
        if (state.orderId() != null) {
            redis.opsForValue().set(ORDER_INDEX_PREFIX + state.orderId(), state.sagaId().toString(), ttl);
        }
    }

    @Override
    public Optional<SagaState> findById(UUID sagaId) {
        Map<Object, Object> entries = redis.opsForHash().entries(stateKey(sagaId));
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromEntries(entries));
    }

    @Override
    public Optional<SagaState> findByOrderId(UUID orderId) {
        String sagaId = redis.opsForValue().get(ORDER_INDEX_PREFIX + orderId);
        if (sagaId == null) {
            return Optional.empty();
        }
        return findById(UUID.fromString(sagaId));
    }

    @Override
    public List<SagaState> findAll() {
        Set<String> keys = redis.keys(STATE_KEY_PREFIX + "*");
        List<SagaState> result = new ArrayList<>();
        if (keys == null) {
            return result;
        }
        for (String key : keys) {
            Map<Object, Object> entries = redis.opsForHash().entries(key);
            if (!entries.isEmpty()) {
                result.add(fromEntries(entries));
            }
        }
        return result;
    }

    @Override
    public void deleteById(UUID sagaId) {
        findById(sagaId).ifPresent(state -> {
            redis.delete(stateKey(sagaId));
            if (state.orderId() != null) {
                redis.delete(ORDER_INDEX_PREFIX + state.orderId());
            }
        });
    }

    private String stateKey(UUID sagaId) {
        return STATE_KEY_PREFIX + sagaId;
    }

    private Duration ttlFor(SagaStatus status) {
        return switch (status) {
            case COMPLETED, COMPENSATED, FAILED -> TERMINAL_TTL;
            case CREATED, RUNNING, COMPENSATING -> ACTIVE_TTL;
        };
    }

    private SagaState fromEntries(Map<Object, Object> e) {
        return new SagaState(
                uuid(e, "sagaId"),
                nullableUuid(e, "orderId"),
                nullableUuid(e, "userId"),
                nullableUuid(e, "eventId"),
                Integer.parseInt(str(e, "quantity")),
                new BigDecimal(str(e, "total")),
                SagaStatus.valueOf(str(e, "status")),
                SagaStep.valueOf(str(e, "currentStep")),
                emptyToNull(str(e, "error")),
                fromJson(str(e, "payload")),
                Instant.ofEpochMilli(Long.parseLong(str(e, "createdAt"))),
                Instant.ofEpochMilli(Long.parseLong(str(e, "updatedAt"))));
    }

    private String str(Map<Object, Object> e, String field) {
        Object value = e.get(field);
        return value != null ? value.toString() : "";
    }

    private UUID uuid(Map<Object, Object> e, String field) {
        return UUID.fromString(str(e, field));
    }

    private UUID nullableUuid(Map<Object, Object> e, String field) {
        String value = str(e, field);
        return value.isEmpty() ? null : UUID.fromString(value);
    }

    private String toJson(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize saga payload", ex);
        }
    }

    private Map<String, String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not deserialize saga payload", ex);
        }
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}