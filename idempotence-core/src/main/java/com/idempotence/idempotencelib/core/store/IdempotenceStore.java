package com.idempotence.idempotencelib.core.store;

import com.idempotence.idempotencelib.core.model.IdempotenceRecord;

public interface IdempotenceStore {
    IdempotenceRecord find(String key);
    boolean tryAcquire(IdempotenceRecord record);
    void save(IdempotenceRecord record);
    void delete(String key);
}
