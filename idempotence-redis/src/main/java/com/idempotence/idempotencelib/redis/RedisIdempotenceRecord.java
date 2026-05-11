package com.idempotence.idempotencelib.redis;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class RedisIdempotenceRecord {

    private String key;
    private String fingerprint;
    private String state;
    private Integer statusCode;
    private Map<String, List<String>> headers;
    private String body;
    private Instant createdAt;
    private Instant expiresAt;

    public RedisIdempotenceRecord() {}

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Map<String, List<String>> getHeaders() { return headers; }
    public void setHeaders(Map<String, List<String>> headers) { this.headers = headers; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
