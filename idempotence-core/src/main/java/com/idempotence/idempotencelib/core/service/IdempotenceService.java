package com.idempotence.idempotencelib.core.service;

import com.idempotence.idempotencelib.core.decision.IdempotenceDecision;
import com.idempotence.idempotencelib.core.model.IdempotenceRecord;
import com.idempotence.idempotencelib.core.model.StoredResponse;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class IdempotenceService {

    private static final Logger log = LoggerFactory.getLogger(IdempotenceService.class);

    private final IdempotenceStore store;
    private final Clock clock;
    private final Duration ttl;

    public IdempotenceService(IdempotenceStore store, Clock clock, Duration ttl) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.ttl = Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) throw new IllegalArgumentException("ttl must be positive");
    }

    public IdempotenceDecision beforeExecution(String key, String fingerprint) {
        validateKey(key);
        validateFingerprint(fingerprint);
        Instant now = Instant.now(clock);
        log.debug("beforeExecution: key={}, fingerprint={}", key, fingerprint);

        IdempotenceRecord existing = store.find(key);
        if (existing != null && existing.isExpired(now)) {
            log.debug("Record expired, deleting: key={}", key);
            store.delete(key);
            existing = null;
        }
        if (existing == null) {
            IdempotenceRecord newRecord = IdempotenceRecord.inProgress(key, fingerprint, now, now.plus(ttl));
            boolean acquired = store.tryAcquire(newRecord);
            if (acquired) {
                log.debug("Acquired new record, decision=PROCEED: key={}", key);
                return IdempotenceDecision.proceed();
            } else {
                log.debug("Failed to acquire record (race condition), decision=IN_PROGRESS: key={}", key);
                return IdempotenceDecision.inProgress("Request is already being processed");
            }
        }
        if (!existing.getFingerprint().equals(fingerprint)) {
            log.warn("Fingerprint mismatch, decision=CONFLICT: key={}", key);
            return IdempotenceDecision.conflict("Idempotency key is already used for another request");
        }
        if (existing.isInProgress()) {
            log.debug("Request still in progress, decision=IN_PROGRESS: key={}", key);
            return IdempotenceDecision.inProgress("Request is still in progress");
        }
        if (existing.isCompleted()) {
            log.debug("Replaying completed response, decision=REPLAY: key={}", key);
            return IdempotenceDecision.replay(existing.getResponse());
        }
        throw new IllegalStateException("Unsupported idempotence state: " + existing.getState());
    }

    public void deleteRecord(String key) {
        validateKey(key);
        log.debug("Deleting idempotence record: key={}", key);
        store.delete(key);
    }

    public void afterExecution(String key, String fingerprint, StoredResponse response) {
        validateKey(key);
        validateFingerprint(fingerprint);
        Objects.requireNonNull(response, "response must not be null");
        Instant now = Instant.now(clock);
        store.save(IdempotenceRecord.completed(key, fingerprint, response, now, now.plus(ttl)));
        log.debug("Response stored as COMPLETED: key={}, status={}", key, response.getStatusCode());
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key must not be blank");
    }

    private void validateFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) throw new IllegalArgumentException("fingerprint must not be blank");
    }
}
