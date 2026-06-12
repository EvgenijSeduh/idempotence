package com.idempotence.idempotencelib.spring.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.annotation.Idempotent;
import com.idempotence.idempotencelib.core.decision.DecisionType;
import com.idempotence.idempotencelib.core.decision.IdempotenceDecision;
import com.idempotence.idempotencelib.core.model.StoredResponse;
import com.idempotence.idempotencelib.core.service.IdempotenceService;
import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IdempotenceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotenceInterceptor.class);

    private final IdempotenceService idempotenceService;
    private final IdempotencyKeyResolver keyResolver;
    private final HttpRequestFingerprintBuilder fingerprintBuilder;
    private final IdempotenceProperties properties;
    private final ObjectMapper objectMapper;

    public IdempotenceInterceptor(IdempotenceService idempotenceService, IdempotencyKeyResolver keyResolver,
                                   HttpRequestFingerprintBuilder fingerprintBuilder,
                                   IdempotenceProperties properties, ObjectMapper objectMapper) {
        this.idempotenceService = Objects.requireNonNull(idempotenceService);
        this.keyResolver = Objects.requireNonNull(keyResolver);
        this.fingerprintBuilder = Objects.requireNonNull(fingerprintBuilder);
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled()) return true;
        if (!(handler instanceof HandlerMethod handlerMethod)) return true;
        if (!isIdempotentHandler(handlerMethod)) return true;

        String key;
        try {
            key = keyResolver.resolve(request);
        } catch (IllegalArgumentException ex) {
            log.debug("Missing idempotency key: uri={}, message={}", request.getRequestURI(), ex.getMessage());
            writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return false;
        }
        if (key == null) return true;
        if (Boolean.TRUE.equals(request.getAttribute(CachedBodyFilter.BODY_TOO_LARGE_ATTRIBUTE))) {
            log.debug(
                    "Request body is too large for idempotency processing: uri={}",
                    request.getRequestURI()
            );

            writeJsonError(
                    response,
                    properties.getPayloadTooLargeStatus(),
                    properties.getPayloadTooLargeMessage()
            );

            return false;
        }

        String fingerprint = fingerprintBuilder.build(request);
        log.debug("Idempotency check: key={}, uri={}", key, request.getRequestURI());
        IdempotenceDecision decision = idempotenceService.beforeExecution(key, fingerprint);

        if (decision.getType() == DecisionType.PROCEED) {
            request.setAttribute(IdempotenceRequestContext.ATTRIBUTE_NAME,
                    new IdempotenceRequestContext(key, fingerprint, true));
            return true;
        }
        if (decision.getType() == DecisionType.REPLAY) {
            writeStoredResponse(response, decision.getStoredResponse());
            return false;
        }
        if (decision.getType() == DecisionType.CONFLICT) {
            writeJsonError(response, properties.getConflictStatus(), properties.getConflictMessage());
            return false;
        }
        if (decision.getType() == DecisionType.IN_PROGRESS) {
            writeJsonError(response, properties.getInProgressStatus(), properties.getInProgressMessage());
            return false;
        }
        throw new IllegalStateException("Unsupported decision type: " + decision.getType());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex == null || !properties.isCleanupOnException()) return;
        Object contextValue = request.getAttribute(IdempotenceRequestContext.ATTRIBUTE_NAME);
        if (!(contextValue instanceof IdempotenceRequestContext context)) return;
        log.warn("Controller threw exception, cleaning up IN_PROGRESS record: key={}", context.getKey());
        idempotenceService.deleteRecord(context.getKey());
        request.removeAttribute(IdempotenceRequestContext.ATTRIBUTE_NAME);
    }

    private boolean isIdempotentHandler(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), Idempotent.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), Idempotent.class);
    }

    private void writeStoredResponse(HttpServletResponse response, StoredResponse storedResponse) throws IOException {
        if (storedResponse == null) throw new IllegalStateException("storedResponse must not be null for REPLAY");
        response.setStatus(storedResponse.getStatusCode());
        Map<String, List<String>> headers = storedResponse.getHeaders();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                for (String value : entry.getValue()) response.addHeader(entry.getKey(), value);
            }
        }
        if (storedResponse.getBody() != null) response.getWriter().write(storedResponse.getBody());
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("status", status, "error", message)));
    }
}
