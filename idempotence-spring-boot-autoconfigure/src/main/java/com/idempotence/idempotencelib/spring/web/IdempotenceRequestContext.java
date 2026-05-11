package com.idempotence.idempotencelib.spring.web;

import java.util.Objects;

public final class IdempotenceRequestContext {

    public static final String ATTRIBUTE_NAME = IdempotenceRequestContext.class.getName() + ".CONTEXT";

    private final String key;
    private final String fingerprint;
    private final boolean shouldStoreResponse;

    public IdempotenceRequestContext(String key, String fingerprint, boolean shouldStoreResponse) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        this.shouldStoreResponse = shouldStoreResponse;
    }

    public String getKey() { return key; }
    public String getFingerprint() { return fingerprint; }
    public boolean isShouldStoreResponse() { return shouldStoreResponse; }
}
