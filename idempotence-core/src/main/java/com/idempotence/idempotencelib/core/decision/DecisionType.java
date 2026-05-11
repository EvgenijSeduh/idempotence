package com.idempotence.idempotencelib.core.decision;

public enum DecisionType {
    PROCEED,
    REPLAY,
    CONFLICT,
    IN_PROGRESS
}
