package com.idempotence.idempotencelib.core.fingerprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DefaultFingerprintServiceTest {

    private final FingerprintService service =
            new DefaultFingerprintService(new ObjectMapper());

    @Test
    void shouldGenerateSameFingerprintForEquivalentJson() {
        FingerprintSource first = new FingerprintSource(
                "POST", "/orders",
                Map.of("expand", List.of("items")),
                "{\"amount\":100,\"currency\":\"RUB\"}"
        );
        FingerprintSource second = new FingerprintSource(
                "POST", "/orders",
                Map.of("expand", List.of("items")),
                "{\"currency\":\"RUB\",\"amount\":100}"
        );
        assertEquals(service.calculate(first), service.calculate(second));
    }

    @Test
    void shouldGenerateDifferentFingerprintForDifferentBody() {
        FingerprintSource first = new FingerprintSource("POST", "/orders", Map.of(), "{\"amount\":100}");
        FingerprintSource second = new FingerprintSource("POST", "/orders", Map.of(), "{\"amount\":200}");
        assertNotEquals(service.calculate(first), service.calculate(second));
    }

    @Test
    void shouldGenerateSameFingerprintForSameQueryParamsInDifferentOrder() {
        FingerprintSource first = new FingerprintSource("GET", "/orders",
                Map.of("b", List.of("2"), "a", List.of("1")), null);
        FingerprintSource second = new FingerprintSource("GET", "/orders",
                Map.of("a", List.of("1"), "b", List.of("2")), null);
        assertEquals(service.calculate(first), service.calculate(second));
    }
}
