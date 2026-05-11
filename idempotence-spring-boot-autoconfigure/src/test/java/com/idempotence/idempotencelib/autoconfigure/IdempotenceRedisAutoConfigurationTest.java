package com.idempotence.idempotencelib.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import com.idempotence.idempotencelib.redis.RedisIdempotenceMapper;
import com.idempotence.idempotencelib.redis.RedisIdempotenceStore;
import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdempotenceRedisAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IdempotenceAutoConfiguration.class,
                    IdempotenceRedisAutoConfiguration.class))
            .withBean(ObjectMapper.class)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void shouldRegisterRedisStoreWhenStringRedisTemplateIsPresent() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(IdempotenceStore.class);
            assertThat(ctx).getBean(IdempotenceStore.class).isInstanceOf(RedisIdempotenceStore.class);
        });
    }

    @Test
    void shouldRegisterRedisIdempotenceMapper() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(RedisIdempotenceMapper.class));
    }

    @Test
    void shouldNotRegisterStoreWhenStringRedisTemplateBeanIsAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IdempotenceRedisAutoConfiguration.class))
                .withBean(ObjectMapper.class)
                .withBean(IdempotenceProperties.class)
                .run(ctx -> assertThat(ctx).doesNotHaveBean(IdempotenceStore.class));
    }

    @Test
    void shouldNotOverrideUserProvidedIdempotenceStore() {
        IdempotenceStore custom = mock(IdempotenceStore.class);
        runner.withBean(IdempotenceStore.class, () -> custom).run(ctx -> {
            assertThat(ctx).hasSingleBean(IdempotenceStore.class);
            assertThat(ctx.getBean(IdempotenceStore.class)).isSameAs(custom);
            assertThat(ctx.getBean(IdempotenceStore.class)).isNotInstanceOf(RedisIdempotenceStore.class);
        });
    }

    @Test
    void shouldNotOverrideUserProvidedRedisIdempotenceMapper() {
        RedisIdempotenceMapper custom = mock(RedisIdempotenceMapper.class);
        runner.withBean(RedisIdempotenceMapper.class, () -> custom).run(ctx -> {
            assertThat(ctx).hasSingleBean(RedisIdempotenceMapper.class);
            assertThat(ctx.getBean(RedisIdempotenceMapper.class)).isSameAs(custom);
        });
    }
}
