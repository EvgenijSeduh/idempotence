package com.idempotence.idempotencelib.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.model.IdempotenceRecord;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RedisIdempotenceStoreFailurePolicyTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper objectMapper;
    private RedisIdempotenceMapper mapper;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        objectMapper = new ObjectMapper();
        mapper = new RedisIdempotenceMapper();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void findShouldReturnNullWhenFailOpen() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        IdempotenceStore store = new RedisIdempotenceStore(redisTemplate, objectMapper, mapper, "idempotence:", RedisFailurePolicy.FAIL_OPEN);
        assertNull(store.find("key-1"));
    }

    @Test
    void tryAcquireShouldReturnTrueWhenFailOpen() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenThrow(new RuntimeException("Redis connection refused"));
        IdempotenceStore store = new RedisIdempotenceStore(redisTemplate, objectMapper, mapper, "idempotence:", RedisFailurePolicy.FAIL_OPEN);
        Instant now = Instant.now();
        assertTrue(store.tryAcquire(IdempotenceRecord.inProgress("key-1", "fp-1", now, now.plusSeconds(60))));
    }

    @Test
    void saveShouldNotThrowWhenFailOpen() {
        doThrow(new RuntimeException("Redis connection refused")).when(valueOps).set(anyString(), anyString(), any());
        IdempotenceStore store = new RedisIdempotenceStore(redisTemplate, objectMapper, mapper, "idempotence:", RedisFailurePolicy.FAIL_OPEN);
        Instant now = Instant.now();
        assertDoesNotThrow(() -> store.save(IdempotenceRecord.inProgress("key-1", "fp-1", now, now.plusSeconds(60))));
    }

    @Test
    void deleteShouldNotThrowWhenFailOpen() {
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        IdempotenceStore store = new RedisIdempotenceStore(redisTemplate, objectMapper, mapper, "idempotence:", RedisFailurePolicy.FAIL_OPEN);
        assertDoesNotThrow(() -> store.delete("key-1"));
    }

    @Test
    void findShouldThrowWhenFailClosed() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        IdempotenceStore store = new RedisIdempotenceStore(redisTemplate, objectMapper, mapper, "idempotence:", RedisFailurePolicy.FAIL_CLOSED);
        assertThrows(IllegalStateException.class, () -> store.find("key-1"));
    }

    @Test
    void tryAcquireShouldThrowWhenFailClosed() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenThrow(new RuntimeException("Redis connection refused"));
        IdempotenceStore store = new RedisIdempotenceStore(redisTemplate, objectMapper, mapper, "idempotence:", RedisFailurePolicy.FAIL_CLOSED);
        Instant now = Instant.now();
        assertThrows(IllegalStateException.class, () -> store.tryAcquire(IdempotenceRecord.inProgress("key-1", "fp-1", now, now.plusSeconds(60))));
    }

    @Test
    void saveShouldThrowWhenFailClosed() {
        doThrow(new RuntimeException("Redis connection refused")).when(valueOps).set(anyString(), anyString(), any());
        IdempotenceStore store = new RedisIdempotenceStore(redisTemplate, objectMapper, mapper, "idempotence:", RedisFailurePolicy.FAIL_CLOSED);
        Instant now = Instant.now();
        assertThrows(IllegalStateException.class, () -> store.save(IdempotenceRecord.inProgress("key-1", "fp-1", now, now.plusSeconds(60))));
    }
}
