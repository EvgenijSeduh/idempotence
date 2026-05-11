package com.idempotence.idempotencelib.spring.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.model.StoredResponse;
import com.idempotence.idempotencelib.core.service.IdempotenceService;
import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
public class IdempotenceResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(IdempotenceResponseAdvice.class);

    private final IdempotenceService idempotenceService;
    private final ObjectMapper objectMapper;
    private final IdempotenceProperties properties;

    public IdempotenceResponseAdvice(IdempotenceService idempotenceService, ObjectMapper objectMapper,
                                      IdempotenceProperties properties) {
        this.idempotenceService = Objects.requireNonNull(idempotenceService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return body;
        if (!(response instanceof ServletServerHttpResponse servletResponse)) return body;

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        Object contextValue = httpRequest.getAttribute(IdempotenceRequestContext.ATTRIBUTE_NAME);
        if (!(contextValue instanceof IdempotenceRequestContext context)) return body;
        if (!context.isShouldStoreResponse()) return body;

        int status = servletResponse.getServletResponse().getStatus();
        List<Integer> storedStatuses = properties.getStoredStatuses();
        if (!storedStatuses.isEmpty() && !storedStatuses.contains(status)) {
            httpRequest.removeAttribute(IdempotenceRequestContext.ATTRIBUTE_NAME);
            return body;
        }

        idempotenceService.afterExecution(context.getKey(), context.getFingerprint(),
                new StoredResponse(status, extractHeaders(response), serializeBody(body)));
        log.debug("Response stored for idempotency: key={}, status={}", context.getKey(), status);
        httpRequest.removeAttribute(IdempotenceRequestContext.ATTRIBUTE_NAME);
        return body;
    }

    private Map<String, List<String>> extractHeaders(ServerHttpResponse response) {
        Set<String> excluded = properties.getStoredResponse().getExcludedHeaders().stream()
                .map(String::toLowerCase).collect(Collectors.toSet());
        Map<String, List<String>> result = new LinkedHashMap<>();
        response.getHeaders().forEach((key, value) -> {
            if (key != null && value != null && !excluded.contains(key.toLowerCase()))
                result.put(key, List.copyOf(value));
        });
        return result;
    }

    private String serializeBody(Object body) {
        if (body == null) return null;
        if (body instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize response body", ex);
        }
    }
}
