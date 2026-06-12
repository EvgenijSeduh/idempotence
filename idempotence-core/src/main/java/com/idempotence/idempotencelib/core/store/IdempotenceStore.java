package com.idempotence.idempotencelib.core.store;

import com.idempotence.idempotencelib.core.model.IdempotenceRecord;
/**
 * In-memory implementation of {@link IdempotenceStore}.
 *
 * <p>This implementation is intended for tests, local development, and
 * single-instance applications only. It does not provide distributed
 * consistency and must not be used as the primary storage in clustered
 * production deployments.</p>
 */
public interface IdempotenceStore {
    IdempotenceRecord find(String key);
    boolean tryAcquire(IdempotenceRecord record);
    void save(IdempotenceRecord record);
    void delete(String key);
}
