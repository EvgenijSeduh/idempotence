package com.idempotence.idempotencelib.redis;

import com.idempotence.idempotencelib.core.model.IdempotenceRecord;
import com.idempotence.idempotencelib.core.model.IdempotenceState;
import com.idempotence.idempotencelib.core.model.StoredResponse;

import java.util.Objects;

public class RedisIdempotenceMapper {

    public RedisIdempotenceRecord toRedis(IdempotenceRecord source) {
        Objects.requireNonNull(source, "source must not be null");
        RedisIdempotenceRecord target = new RedisIdempotenceRecord();
        target.setKey(source.getKey());
        target.setFingerprint(source.getFingerprint());
        target.setState(source.getState().name());
        target.setCreatedAt(source.getCreatedAt());
        target.setExpiresAt(source.getExpiresAt());
        if (source.getResponse() != null) {
            target.setStatusCode(source.getResponse().getStatusCode());
            target.setHeaders(source.getResponse().getHeaders());
            target.setBody(source.getResponse().getBody());
        }
        return target;
    }

    public IdempotenceRecord fromRedis(RedisIdempotenceRecord source) {
        Objects.requireNonNull(source, "source must not be null");
        IdempotenceState state = IdempotenceState.valueOf(source.getState());
        if (state == IdempotenceState.IN_PROGRESS) {
            return IdempotenceRecord.inProgress(
                    source.getKey(), source.getFingerprint(),
                    source.getCreatedAt(), source.getExpiresAt());
        }
        return IdempotenceRecord.completed(
                source.getKey(), source.getFingerprint(),
                new StoredResponse(source.getStatusCode(), source.getHeaders(), source.getBody()),
                source.getCreatedAt(), source.getExpiresAt());
    }
}
