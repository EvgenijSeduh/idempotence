package com.idempotence.idempotencelib.spring.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestWebConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdempotenceConcurrentWebIT {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private TestOrderController controller;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        controller.resetCounter();
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        controller.resetCounter();
    }

    @Test
    void shouldReturnInProgressForSecondConcurrentRequest() throws Exception {
        String key = "test-key-in-progress";
        String url = "http://localhost:" + port + "/orders/slow";
        controller.prepareSlowRequestBlocking();

        CompletableFuture<ResponseEntity<Map>> firstRequest = CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Idempotency-Key", key);
            return restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>("{\"amount\": 777}", headers), Map.class);
        });

        assertTrue(controller.awaitSlowRequestStarted(2000), "First request did not reach controller in time");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        ResponseEntity<Map> secondResponse = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>("{\"amount\": 777}", headers), Map.class);

        assertEquals(HttpStatus.CONFLICT, secondResponse.getStatusCode());
        assertEquals(409, secondResponse.getBody().get("status"));

        controller.releaseSlowRequest();
        ResponseEntity<Map> firstResponse = firstRequest.get(5, TimeUnit.SECONDS);
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());
        assertEquals(1, controller.getCounter());
    }
}
