package com.idempotence.idempotencelib.core.fingerprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultFingerprintService implements FingerprintService {

    private final ObjectMapper objectMapper;

    public DefaultFingerprintService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public String calculate(FingerprintSource source) {
        Objects.requireNonNull(source, "source must not be null");

        String rawFingerprint = normalizeMethod(source.httpMethod()) + "\n"
                + normalizePath(source.path()) + "\n"
                + normalizeQuery(source.queryParams()) + "\n"
                + normalizeBody(source.body());

        return sha256(rawFingerprint);
    }

    private String normalizeMethod(String httpMethod) {
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("httpMethod must not be blank");
        }
        return httpMethod.trim().toUpperCase();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        return path.trim();
    }

    private String normalizeQuery(Map<String, List<String>> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) return "";

        Map<String, List<String>> sorted = new TreeMap<>();
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            List<String> values = entry.getValue() == null ? List.of()
                    : entry.getValue().stream().filter(Objects::nonNull).sorted().toList();
            sorted.put(entry.getKey(), values);
        }

        return sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String normalizeBody(String body) {
        if (body == null || body.isBlank()) return "";
        String trimmed = body.trim();
        try {
            JsonNode parsed = objectMapper.readTree(trimmed);
            return objectMapper.writeValueAsString(canonicalize(parsed));
        } catch (JsonProcessingException ex) {
            return trimmed;
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            List<String> fieldNames = new ArrayList<>();
            node.fieldNames().forEachRemaining(fieldNames::add);
            fieldNames.sort(Comparator.naturalOrder());
            for (String fieldName : fieldNames) {
                result.set(fieldName, canonicalize(node.get(fieldName)));
            }
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            for (JsonNode child : node) result.add(canonicalize(child));
            return result;
        }
        return node;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
