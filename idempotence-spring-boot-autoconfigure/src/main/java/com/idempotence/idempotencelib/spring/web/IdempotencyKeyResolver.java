package com.idempotence.idempotencelib.spring.web;

import jakarta.servlet.http.HttpServletRequest;

public interface IdempotencyKeyResolver {
    String resolve(HttpServletRequest request);
}
