package com.idempotence.idempotencelib.spring.properties;

import com.idempotence.idempotencelib.redis.RedisFailurePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "idempotence")
public class IdempotenceProperties {

    private boolean enabled = true;
    private String headerName = "Idempotency-Key";
    private boolean requireKey = true;
    private Duration ttl = Duration.ofHours(24);
    private int conflictStatus = 409;
    private int inProgressStatus = 409;
    private String missingKeyMessage = "Idempotency key is required";
    private String conflictMessage = "Idempotency key is already used for another request";
    private String inProgressMessage = "Request is still in progress";
    private String keyPrefix = "idempotence:";
    private long maxBodySize = 1_048_576L;
    private int payloadTooLargeStatus = 413;
    private String payloadTooLargeMessage = "Request body is too large for idempotency processing";
    private List<Integer> storedStatuses = List.of();
    private StoredResponseProperties storedResponse = new StoredResponseProperties();
    private boolean cleanupOnException = true;
    private RedisFailurePolicy redisFailurePolicy = RedisFailurePolicy.FAIL_CLOSED;

    public static class StoredResponseProperties {
        private List<String> excludedHeaders = List.of("Authorization", "Cookie", "Set-Cookie");

        public List<String> getExcludedHeaders() { return excludedHeaders; }
        public void setExcludedHeaders(List<String> excludedHeaders) {
            this.excludedHeaders = excludedHeaders == null ? List.of() : List.copyOf(excludedHeaders);
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHeaderName() { return headerName; }
    public void setHeaderName(String headerName) { this.headerName = headerName; }
    public boolean isRequireKey() { return requireKey; }
    public void setRequireKey(boolean requireKey) { this.requireKey = requireKey; }
    public Duration getTtl() { return ttl; }
    public void setTtl(Duration ttl) { this.ttl = ttl; }
    public int getConflictStatus() { return conflictStatus; }
    public void setConflictStatus(int conflictStatus) { this.conflictStatus = conflictStatus; }
    public int getInProgressStatus() { return inProgressStatus; }
    public void setInProgressStatus(int inProgressStatus) { this.inProgressStatus = inProgressStatus; }
    public String getMissingKeyMessage() { return missingKeyMessage; }
    public void setMissingKeyMessage(String missingKeyMessage) { this.missingKeyMessage = missingKeyMessage; }
    public String getConflictMessage() { return conflictMessage; }
    public void setConflictMessage(String conflictMessage) { this.conflictMessage = conflictMessage; }
    public String getInProgressMessage() { return inProgressMessage; }
    public void setInProgressMessage(String inProgressMessage) { this.inProgressMessage = inProgressMessage; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public long getMaxBodySize() { return maxBodySize; }
    public void setMaxBodySize(long maxBodySize) { this.maxBodySize = maxBodySize; }
    public int getPayloadTooLargeStatus() { return payloadTooLargeStatus;}
    public void setPayloadTooLargeStatus(int payloadTooLargeStatus) { this.payloadTooLargeStatus = payloadTooLargeStatus;}
    public String getPayloadTooLargeMessage() { return payloadTooLargeMessage;}
    public void setPayloadTooLargeMessage(String payloadTooLargeMessage) { this.payloadTooLargeMessage = payloadTooLargeMessage;}
    public List<Integer> getStoredStatuses() { return storedStatuses; }
    public void setStoredStatuses(List<Integer> storedStatuses) {
        this.storedStatuses = storedStatuses == null ? List.of() : List.copyOf(storedStatuses);
    }
    public StoredResponseProperties getStoredResponse() { return storedResponse; }
    public void setStoredResponse(StoredResponseProperties storedResponse) { this.storedResponse = storedResponse; }
    public boolean isCleanupOnException() { return cleanupOnException; }
    public void setCleanupOnException(boolean cleanupOnException) { this.cleanupOnException = cleanupOnException; }
    public RedisFailurePolicy getRedisFailurePolicy() { return redisFailurePolicy; }
    public void setRedisFailurePolicy(RedisFailurePolicy redisFailurePolicy) { this.redisFailurePolicy = redisFailurePolicy; }
}
