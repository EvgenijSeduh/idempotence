package com.idempotence.idempotencelib.core.service;

import com.idempotence.idempotencelib.core.decision.DecisionType;
import com.idempotence.idempotencelib.core.decision.IdempotenceDecision;
import com.idempotence.idempotencelib.core.model.StoredResponse;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import com.idempotence.idempotencelib.core.store.InMemoryIdempotenceStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IdempotenceServiceTest {

    @Test
    void shouldProceedForFirstRequest() {
        IdempotenceService service = new IdempotenceService(
                new InMemoryIdempotenceStore(), Clock.systemUTC(), Duration.ofMinutes(10));
        assertEquals(DecisionType.PROCEED, service.beforeExecution("key-1", "fp-1").getType());
    }

    @Test
    void shouldReplayCompletedRequest() {
        IdempotenceStore store = new InMemoryIdempotenceStore();
        IdempotenceService service = new IdempotenceService(store, Clock.systemUTC(), Duration.ofMinutes(10));
        service.beforeExecution("key-1", "fp-1");
        service.afterExecution("key-1", "fp-1",
                new StoredResponse(201, Map.of("Content-Type", List.of("application/json")), "{\"id\":1}"));
        IdempotenceDecision decision = service.beforeExecution("key-1", "fp-1");
        assertEquals(DecisionType.REPLAY, decision.getType());
        assertNotNull(decision.getStoredResponse());
        assertEquals(201, decision.getStoredResponse().getStatusCode());
        assertEquals("{\"id\":1}", decision.getStoredResponse().getBody());
    }

    @Test
    void shouldReturnConflictWhenFingerprintDiffers() {
        IdempotenceService service = new IdempotenceService(
                new InMemoryIdempotenceStore(), Clock.systemUTC(), Duration.ofMinutes(10));
        service.beforeExecution("key-1", "fp-1");
        assertEquals(DecisionType.CONFLICT, service.beforeExecution("key-1", "fp-2").getType());
    }

    @Test
    void shouldReturnInProgressWhenRequestIsStillRunning() {
        IdempotenceService service = new IdempotenceService(
                new InMemoryIdempotenceStore(), Clock.systemUTC(), Duration.ofMinutes(10));
        service.beforeExecution("key-1", "fp-1");
        assertEquals(DecisionType.IN_PROGRESS, service.beforeExecution("key-1", "fp-1").getType());
    }

    @Test
    void shouldAllowRetryAfterRecordDeleted() {
        IdempotenceService service = new IdempotenceService(
                new InMemoryIdempotenceStore(), Clock.systemUTC(), Duration.ofMinutes(10));
        service.beforeExecution("key-1", "fp-1");
        service.deleteRecord("key-1");
        assertEquals(DecisionType.PROCEED, service.beforeExecution("key-1", "fp-1").getType());
    }
}
