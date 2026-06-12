package com.idempotence.idempotencelib.spring.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = TestWebConfiguration.class)
@AutoConfigureMockMvc
class IdempotenceWebFlowTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.2-alpine");

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(REDIS_IMAGE)
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("idempotence.max-body-size", () -> "20");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TestOrderController controller;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        controller.resetCounter();
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void shouldReplaySecondRequestWithSameKey() throws Exception {
        String key = "test-key-1";
        String body = "{\"amount\": 100}";

        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.callCount").value(1));

        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.callCount").value(1));

        assertEquals(1, controller.getCounter());
    }

    @Test
    void shouldReturnConflictForSameKeyButDifferentBody() throws Exception {
        String key = "test-key-conflict";

        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content("{\"amount\": 100}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content("{\"amount\": 200}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertEquals(1, controller.getCounter());
    }

    @Test
    void shouldReturnBadRequestWhenIdempotencyKeyIsMissing() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\": 100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertEquals(0, controller.getCounter());
    }

    @Test
    void shouldReturnPayloadTooLargeForLargeIdempotentRequestBody() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(APPLICATION_JSON)
                        .header("Idempotency-Key", "test-key-large-body")
                        .content("{\"amount\": 100, \"description\": \"too large body\"}"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));

        assertEquals(0, controller.getCounter());
    }
}
