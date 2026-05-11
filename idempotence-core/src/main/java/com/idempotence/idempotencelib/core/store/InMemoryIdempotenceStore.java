package com.idempotence.idempotencelib.core.store;

import com.idempotence.idempotencelib.core.model.IdempotenceRecord;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryIdempotenceStore implements IdempotenceStore {

    private final ConcurrentMap<String, IdempotenceRecord> records = new ConcurrentHashMap<>();

    @Override
    public IdempotenceRecord find(String key) { return records.get(key); }

    @Override
    public boolean tryAcquire(IdempotenceRecord record) {
        return records.putIfAbsent(record.getKey(), record) == null;
    }

    @Override
    public void save(IdempotenceRecord record) { records.put(record.getKey(), record); }

    @Override
    public void delete(String key) { records.remove(key); }
}
