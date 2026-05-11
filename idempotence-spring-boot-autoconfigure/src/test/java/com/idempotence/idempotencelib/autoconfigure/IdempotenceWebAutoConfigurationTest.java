package com.idempotence.idempotencelib.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.spring.web.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdempotenceWebAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IdempotenceAutoConfiguration.class,
                    IdempotenceRedisAutoConfiguration.class,
                    IdempotenceWebAutoConfiguration.class,
                    WebMvcAutoConfiguration.class))
            .withBean(ObjectMapper.class)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void shouldRegisterAllWebBeans() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(IdempotencyKeyResolver.class);
            assertThat(ctx).hasSingleBean(HttpRequestFingerprintBuilder.class);
            assertThat(ctx).hasSingleBean(IdempotenceInterceptor.class);
            assertThat(ctx).hasSingleBean(IdempotenceResponseAdvice.class);
        });
    }

    @Test
    void shouldRegisterCachedBodyFilter() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(FilterRegistrationBean.class);
            assertThat(ctx.getBean(FilterRegistrationBean.class).getFilter()).isInstanceOf(CachedBodyFilter.class);
        });
    }

    @Test
    void shouldRegisterDefaultIdempotencyKeyResolver() {
        runner.run(ctx -> assertThat(ctx).getBean(IdempotencyKeyResolver.class)
                .isInstanceOf(DefaultIdempotencyKeyResolver.class));
    }

    @Test
    void shouldNotOverrideUserProvidedIdempotencyKeyResolver() {
        IdempotencyKeyResolver custom = mock(IdempotencyKeyResolver.class);
        runner.withBean(IdempotencyKeyResolver.class, () -> custom).run(ctx -> {
            assertThat(ctx).hasSingleBean(IdempotencyKeyResolver.class);
            assertThat(ctx.getBean(IdempotencyKeyResolver.class)).isSameAs(custom);
            assertThat(ctx.getBean(IdempotencyKeyResolver.class)).isNotInstanceOf(DefaultIdempotencyKeyResolver.class);
        });
    }

    @Test
    void shouldNotOverrideUserProvidedIdempotenceInterceptor() {
        IdempotenceInterceptor custom = mock(IdempotenceInterceptor.class);
        runner.withBean(IdempotenceInterceptor.class, () -> custom).run(ctx -> {
            assertThat(ctx).hasSingleBean(IdempotenceInterceptor.class);
            assertThat(ctx.getBean(IdempotenceInterceptor.class)).isSameAs(custom);
        });
    }
}
