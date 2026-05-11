package com.idempotence.idempotencelib.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.model.IdempotenceRecord;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class RedisIdempotenceStore implements IdempotenceStore {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotenceStore.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisIdempotenceMapper mapper;
    private final String keyPrefix;
    private final RedisFailurePolicy failurePolicy;

    public RedisIdempotenceStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                  RedisIdempotenceMapper mapper) {
        this(redisTemplate, objectMapper, mapper, "idempotence:", RedisFailurePolicy.FAIL_CLOSED);
    }

    public RedisIdempotenceStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                  RedisIdempotenceMapper mapper, String keyPrefix) {
        this(redisTemplate, objectMapper, mapper, keyPrefix, RedisFailurePolicy.FAIL_CLOSED);
    }

    public RedisIdempotenceStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                  RedisIdempotenceMapper mapper, String keyPrefix,
                                  RedisFailurePolicy failurePolicy) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.keyPrefix = Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");
        this.failurePolicy = Objects.requireNonNull(failurePolicy, "failurePolicy must not be null");
    }

    @Override
    public IdempotenceRecord find(String key) {
        validateKey(key);
        try {
            String value = redisTemplate.opsForValue().get(buildRedisKey(key));
            if (value == null) {
                log.debug("Record not found in Redis: key={}", key);
                return null;
            }
            RedisIdempotenceRecord redisRecord = objectMapper.readValue(value, RedisIdempotenceRecord.class);
            log.debug("Record found in Redis: key={}, state={}", key, redisRecord.getState());
            return mapper.fromRedis(redisRecord);
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize Redis record: key={}", key, ex);
            throw new IllegalStateException("Failed to deserialize Redis idempotence record", ex);
        } catch (Exception ex) {
            return handleRedisFailure("find", key, ex, null);
        }
    }

    @Override
    public boolean tryAcquire(IdempotenceRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(buildRedisKey(record.getKey()), serialize(record), calculateTtl(record.getExpiresAt()));
            boolean acquired = Boolean.TRUE.equals(success);
            log.debug("tryAcquire: key={}, acquired={}", record.getKey(), acquired);
            return acquired;
        } catch (Exception ex) {
            return handleRedisFailure("tryAcquire", record.getKey(), ex, true);
        }
    }

    @Override
    public void save(IdempotenceRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        try {
            redisTemplate.opsForValue().set(
                    buildRedisKey(record.getKey()), serialize(record), calculateTtl(record.getExpiresAt()));
            log.debug("Record saved to Redis: key={}, state={}", record.getKey(), record.getState());
        } catch (Exception ex) {
            handleRedisFailure("save", record.getKey(), ex, null);
        }
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        try {
            redisTemplate.delete(buildRedisKey(key));
            log.debug("Record deleted from Redis: key={}", key);
        } catch (Exception ex) {
            handleRedisFailure("delete", key, ex, null);
        }
    }

    private <T> T handleRedisFailure(String operation, String key, Exception ex, T fallback) {
        if (failurePolicy == RedisFailurePolicy.FAIL_OPEN) {
            log.warn("Redis unavailable (FAIL_OPEN), skipping idempotency for operation={}, key={}: {}",
                    operation, key, ex.getMessage());
            return fallback;
        }
        log.error("Redis unavailable (FAIL_CLOSED), blocking request for operation={}, key={}", operation, key, ex);
        throw new IllegalStateException("Idempotency store unavailable: " + ex.getMessage(), ex);
    }

    private String serialize(IdempotenceRecord record) {
        try {
            return objectMapper.writeValueAsString(mapper.toRedis(record));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize Redis record: key={}", record.getKey(), ex);
            throw new IllegalStateException("Failed to serialize Redis idempotence record", ex);
        }
    }

    private Duration calculateTtl(Instant expiresAt) {
        Instant now = Instant.now();
        return expiresAt.isAfter(now) ? Duration.between(now, expiresAt) : Duration.ofSeconds(1);
    }

    private String buildRedisKey(String key) { return keyPrefix + key; }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key must not be blank");
    }
}
