package com.idempotence.idempotencelib.spring.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.fingerprint.DefaultFingerprintService;
import com.idempotence.idempotencelib.core.fingerprint.FingerprintService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HttpRequestFingerprintBuilderTest {

    private final FingerprintService fingerprintService = new DefaultFingerprintService(new ObjectMapper());
    private final HttpRequestFingerprintBuilder builder = new HttpRequestFingerprintBuilder(fingerprintService);

    @Test
    void shouldBuildSameFingerprintForEquivalentRequests() throws Exception {
        MockHttpServletRequest raw1 = new MockHttpServletRequest("POST", "/orders");
        raw1.setCharacterEncoding("UTF-8");
        raw1.setContentType("application/json");
        raw1.setContent("{\"amount\":100,\"currency\":\"RUB\"}".getBytes());

        MockHttpServletRequest raw2 = new MockHttpServletRequest("POST", "/orders");
        raw2.setCharacterEncoding("UTF-8");
        raw2.setContentType("application/json");
        raw2.setContent("{\"currency\":\"RUB\",\"amount\":100}".getBytes());

        assertEquals(builder.build(new CachedBodyHttpServletRequest(raw1)),
                     builder.build(new CachedBodyHttpServletRequest(raw2)));
    }

    @Test
    void shouldBuildDifferentFingerprintForDifferentBodies() throws Exception {
        MockHttpServletRequest raw1 = new MockHttpServletRequest("POST", "/orders");
        raw1.setCharacterEncoding("UTF-8");
        raw1.setContent("{\"amount\":100}".getBytes());

        MockHttpServletRequest raw2 = new MockHttpServletRequest("POST", "/orders");
        raw2.setCharacterEncoding("UTF-8");
        raw2.setContent("{\"amount\":200}".getBytes());

        assertNotEquals(builder.build(new CachedBodyHttpServletRequest(raw1)),
                        builder.build(new CachedBodyHttpServletRequest(raw2)));
    }
}
