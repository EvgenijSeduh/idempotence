package com.idempotence.idempotencelib.core.model;

import java.time.Instant;
import java.util.Objects;

public final class IdempotenceRecord {
    private final String key;
    private final String fingerprint;
    private final IdempotenceState state;
    private final StoredResponse response;
    private final Instant createdAt;
    private final Instant expiresAt;

    public IdempotenceRecord(String key, String fingerprint, IdempotenceState state,
                             StoredResponse response, Instant createdAt, Instant expiresAt) {
        this.key = requireNotBlank(key, "key must not be blank");
        this.fingerprint = requireNotBlank(fingerprint, "fingerprint must not be blank");
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.response = response;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (expiresAt.isBefore(createdAt)) throw new IllegalArgumentException("expiresAt must not be before createdAt");
        if (state == IdempotenceState.COMPLETED && response == null)
            throw new IllegalArgumentException("response must not be null for COMPLETED state");
    }

    public static IdempotenceRecord inProgress(String key, String fingerprint, Instant createdAt, Instant expiresAt) {
        return new IdempotenceRecord(key, fingerprint, IdempotenceState.IN_PROGRESS, null, createdAt, expiresAt);
    }

    public static IdempotenceRecord completed(String key, String fingerprint, StoredResponse response,
                                              Instant createdAt, Instant expiresAt) {
        return new IdempotenceRecord(key, fingerprint, IdempotenceState.COMPLETED,
                Objects.requireNonNull(response), createdAt, expiresAt);
    }

    public String getKey() { return key; }
    public String getFingerprint() { return fingerprint; }
    public IdempotenceState getState() { return state; }
    public StoredResponse getResponse() { return response; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isInProgress() { return state == IdempotenceState.IN_PROGRESS; }
    public boolean isCompleted() { return state == IdempotenceState.COMPLETED; }
    public boolean isExpired(Instant now) { return !expiresAt.isAfter(Objects.requireNonNull(now)); }

    public IdempotenceRecord withCompletedResponse(StoredResponse response) {
        return new IdempotenceRecord(key, fingerprint, IdempotenceState.COMPLETED,
                Objects.requireNonNull(response), createdAt, expiresAt);
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }

    @Override
    public String toString() {
        return "IdempotenceRecord{key='" + key + "', fingerprint='" + fingerprint +
                "', state=" + state + ", createdAt=" + createdAt + ", expiresAt=" + expiresAt + '}';
    }
}
