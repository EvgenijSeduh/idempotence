package com.idempotence.idempotencelib.core.decision;

import com.idempotence.idempotencelib.core.model.StoredResponse;

public final class IdempotenceDecision {
    private final DecisionType type;
    private final StoredResponse storedResponse;
    private final String message;

    private IdempotenceDecision(DecisionType type, StoredResponse storedResponse, String message) {
        this.type = type;
        this.storedResponse = storedResponse;
        this.message = message;
    }

    public static IdempotenceDecision proceed() {
        return new IdempotenceDecision(DecisionType.PROCEED, null, null);
    }

    public static IdempotenceDecision replay(StoredResponse storedResponse) {
        return new IdempotenceDecision(DecisionType.REPLAY, storedResponse, null);
    }

    public static IdempotenceDecision conflict(String message) {
        return new IdempotenceDecision(DecisionType.CONFLICT, null, message);
    }

    public static IdempotenceDecision inProgress(String message) {
        return new IdempotenceDecision(DecisionType.IN_PROGRESS, null, message);
    }

    public DecisionType getType() { return type; }
    public StoredResponse getStoredResponse() { return storedResponse; }
    public String getMessage() { return message; }
}
