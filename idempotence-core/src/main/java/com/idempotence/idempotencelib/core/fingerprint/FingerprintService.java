package com.idempotence.idempotencelib.core.fingerprint;

public interface FingerprintService {
    String calculate(FingerprintSource source);
}
