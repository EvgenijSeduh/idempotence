package com.idempotence.idempotencelib.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.fingerprint.DefaultFingerprintService;
import com.idempotence.idempotencelib.core.fingerprint.FingerprintService;
import com.idempotence.idempotencelib.core.service.IdempotenceService;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdempotenceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotenceAutoConfiguration.class))
            .withBean(ObjectMapper.class)
            .withBean(IdempotenceStore.class, () -> mock(IdempotenceStore.class));

    @Test
    void shouldRegisterAllCoreBeansByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Clock.class);
            assertThat(ctx).hasSingleBean(FingerprintService.class);
            assertThat(ctx).hasSingleBean(IdempotenceService.class);
            assertThat(ctx).hasSingleBean(IdempotenceProperties.class);
        });
    }

    @Test
    void shouldRegisterDefaultFingerprintService() {
        runner.run(ctx -> assertThat(ctx).getBean(FingerprintService.class)
                .isInstanceOf(DefaultFingerprintService.class));
    }

    @Test
    void shouldNotOverrideUserProvidedClock() {
        Clock customClock = Clock.systemUTC();
        runner.withBean("customClock", Clock.class, () -> customClock).run(ctx -> {
            assertThat(ctx).hasSingleBean(Clock.class);
            assertThat(ctx.getBean(Clock.class)).isSameAs(customClock);
        });
    }

    @Test
    void shouldNotOverrideUserProvidedFingerprintService() {
        FingerprintService custom = mock(FingerprintService.class);
        runner.withBean(FingerprintService.class, () -> custom).run(ctx -> {
            assertThat(ctx).hasSingleBean(FingerprintService.class);
            assertThat(ctx.getBean(FingerprintService.class)).isSameAs(custom);
            assertThat(ctx.getBean(FingerprintService.class)).isNotInstanceOf(DefaultFingerprintService.class);
        });
    }

    @Test
    void shouldNotOverrideUserProvidedIdempotenceService() {
        IdempotenceService custom = mock(IdempotenceService.class);
        runner.withBean(IdempotenceService.class, () -> custom).run(ctx -> {
            assertThat(ctx).hasSingleBean(IdempotenceService.class);
            assertThat(ctx.getBean(IdempotenceService.class)).isSameAs(custom);
        });
    }
}
