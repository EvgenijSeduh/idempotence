package com.idempotence.idempotencelib.redis;

import com.idempotence.idempotencelib.core.model.IdempotenceRecord;
import com.idempotence.idempotencelib.core.model.StoredResponse;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class RedisIdempotenceStoreIT {

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private IdempotenceStore store;

    @BeforeEach
    void setUp() {
        connectionFactory = RedisTestSupport.createConnectionFactory();
        redisTemplate = RedisTestSupport.createRedisTemplate(connectionFactory);
        store = new RedisIdempotenceStore(redisTemplate, RedisTestSupport.createObjectMapper(), new RedisIdempotenceMapper());
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null) redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @Test
    void shouldAcquireAndFindInProgressRecord() {
        Instant now = Instant.now();
        IdempotenceRecord record = IdempotenceRecord.inProgress("key-1", "fp-1", now, now.plusSeconds(60));
        assertTrue(store.tryAcquire(record));
        IdempotenceRecord saved = store.find("key-1");
        assertNotNull(saved);
        assertEquals("key-1", saved.getKey());
        assertTrue(saved.isInProgress());
    }

    @Test
    void shouldSaveCompletedRecordAndReadItBack() {
        Instant now = Instant.now();
        IdempotenceRecord record = IdempotenceRecord.completed("key-2", "fp-2",
                new StoredResponse(201, Map.of("Content-Type", List.of("application/json")), "{\"id\":42}"),
                now, now.plusSeconds(60));
        store.save(record);
        IdempotenceRecord saved = store.find("key-2");
        assertNotNull(saved);
        assertTrue(saved.isCompleted());
        assertEquals(201, saved.getResponse().getStatusCode());
        assertEquals("{\"id\":42}", saved.getResponse().getBody());
    }

    @Test
    void shouldExpireRecordByTtl() throws InterruptedException {
        Instant now = Instant.now();
        store.tryAcquire(IdempotenceRecord.inProgress("key-ttl", "fp-ttl", now, now.plusSeconds(1)));
        Thread.sleep(1500);
        assertNull(store.find("key-ttl"));
    }

    @Test
    void shouldAllowOnlyOneWinnerDuringConcurrentAcquire() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            Instant now = Instant.now();
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return store.tryAcquire(IdempotenceRecord.inProgress("concurrent-key", "same-fp", now, now.plusSeconds(60)));
                }));
            }
            ready.await();
            start.countDown();
            int successCount = 0;
            for (Future<Boolean> f : futures) if (f.get()) successCount++;
            assertEquals(1, successCount);
        } finally {
            executor.shutdownNow();
        }
    }
}
