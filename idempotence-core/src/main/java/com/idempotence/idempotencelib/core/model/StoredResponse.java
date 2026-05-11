package com.idempotence.idempotencelib.core.model;

import java.util.List;
import java.util.Map;

public final class StoredResponse {

    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;

    public StoredResponse(int statusCode, Map<String, List<String>> headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
        this.body = body;
    }

    public int getStatusCode() { return statusCode; }
    public Map<String, List<String>> getHeaders() { return headers; }
    public String getBody() { return body; }
}
