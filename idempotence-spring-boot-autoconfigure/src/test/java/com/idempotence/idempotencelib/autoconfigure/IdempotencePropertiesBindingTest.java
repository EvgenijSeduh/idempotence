package com.idempotence.idempotencelib.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import com.idempotence.idempotencelib.redis.RedisFailurePolicy;
import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdempotencePropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotenceAutoConfiguration.class))
            .withBean(ObjectMapper.class)
            .withBean(IdempotenceStore.class, () -> mock(IdempotenceStore.class));

    @Test
    void shouldHaveCorrectDefaultValues() {
        runner.run(ctx -> {
            IdempotenceProperties props = ctx.getBean(IdempotenceProperties.class);
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getHeaderName()).isEqualTo("Idempotency-Key");
            assertThat(props.isRequireKey()).isTrue();
            assertThat(props.getTtl()).isEqualTo(Duration.ofHours(24));
            assertThat(props.getConflictStatus()).isEqualTo(409);
            assertThat(props.getInProgressStatus()).isEqualTo(409);
            assertThat(props.getKeyPrefix()).isEqualTo("idempotence:");
            assertThat(props.getMaxBodySize()).isEqualTo(1_048_576L);
            assertThat(props.isCleanupOnException()).isTrue();
            assertThat(props.getRedisFailurePolicy()).isEqualTo(RedisFailurePolicy.FAIL_CLOSED);
            assertThat(props.getStoredStatuses()).isEmpty();
        });
    }

    @Test
    void shouldBindCustomValues() {
        runner.withPropertyValues(
                "idempotence.enabled=false",
                "idempotence.header-name=X-Request-Id",
                "idempotence.require-key=false",
                "idempotence.ttl=1h",
                "idempotence.conflict-status=422",
                "idempotence.in-progress-status=423",
                "idempotence.key-prefix=my-app:",
                "idempotence.max-body-size=512",
                "idempotence.cleanup-on-exception=false",
                "idempotence.redis-failure-policy=FAIL_OPEN"
        ).run(ctx -> {
            IdempotenceProperties props = ctx.getBean(IdempotenceProperties.class);
            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getHeaderName()).isEqualTo("X-Request-Id");
            assertThat(props.isRequireKey()).isFalse();
            assertThat(props.getTtl()).isEqualTo(Duration.ofHours(1));
            assertThat(props.getConflictStatus()).isEqualTo(422);
            assertThat(props.getInProgressStatus()).isEqualTo(423);
            assertThat(props.getKeyPrefix()).isEqualTo("my-app:");
            assertThat(props.getMaxBodySize()).isEqualTo(512L);
            assertThat(props.isCleanupOnException()).isFalse();
            assertThat(props.getRedisFailurePolicy()).isEqualTo(RedisFailurePolicy.FAIL_OPEN);
        });
    }

    @Test
    void shouldBindExcludedHeaders() {
        runner.withPropertyValues("idempotence.stored-response.excluded-headers=X-Custom,Authorization")
                .run(ctx -> assertThat(ctx.getBean(IdempotenceProperties.class)
                        .getStoredResponse().getExcludedHeaders())
                        .containsExactlyInAnyOrder("X-Custom", "Authorization"));
    }

    @Test
    void shouldBindStoredStatuses() {
        runner.withPropertyValues("idempotence.stored-statuses=200,201,202")
                .run(ctx -> assertThat(ctx.getBean(IdempotenceProperties.class).getStoredStatuses())
                        .containsExactlyInAnyOrder(200, 201, 202));
    }
}
