package com.idempotence.idempotencelib.spring.web;

import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;

public class DefaultIdempotencyKeyResolver implements IdempotencyKeyResolver {

    private final IdempotenceProperties properties;

    public DefaultIdempotencyKeyResolver(IdempotenceProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public String resolve(HttpServletRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String value = request.getHeader(properties.getHeaderName());
        if (value == null || value.isBlank()) {
            if (properties.isRequireKey()) throw new IllegalArgumentException(properties.getMissingKeyMessage());
            return null;
        }
        return value.trim();
    }
}
