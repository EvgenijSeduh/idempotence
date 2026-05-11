package com.idempotence.idempotencelib.spring.web;

import com.idempotence.idempotencelib.core.fingerprint.FingerprintService;
import com.idempotence.idempotencelib.core.fingerprint.FingerprintSource;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HttpRequestFingerprintBuilder {

    private final FingerprintService fingerprintService;

    public HttpRequestFingerprintBuilder(FingerprintService fingerprintService) {
        this.fingerprintService = Objects.requireNonNull(fingerprintService, "fingerprintService must not be null");
    }

    public String build(HttpServletRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return fingerprintService.calculate(new FingerprintSource(
                request.getMethod(),
                request.getRequestURI(),
                extractQueryParams(request),
                extractBody(request)
        ));
    }

    private Map<String, List<String>> extractQueryParams(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue())));
    }

    private String extractBody(HttpServletRequest request) {
        if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
            return cachedRequest.getCachedBodyAsString();
        }
        return "";
    }
}
