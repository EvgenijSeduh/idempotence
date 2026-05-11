package com.idempotence.idempotencelib.core.fingerprint;

import java.util.List;
import java.util.Map;

public record FingerprintSource(
        String httpMethod,
        String path,
        Map<String, List<String>> queryParams,
        String body
) {
}
