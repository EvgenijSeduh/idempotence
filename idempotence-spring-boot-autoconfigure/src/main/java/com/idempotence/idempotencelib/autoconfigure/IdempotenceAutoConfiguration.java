package com.idempotence.idempotencelib.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.fingerprint.DefaultFingerprintService;
import com.idempotence.idempotencelib.core.fingerprint.FingerprintService;
import com.idempotence.idempotencelib.core.service.IdempotenceService;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@AutoConfiguration
@EnableConfigurationProperties(IdempotenceProperties.class)
public class IdempotenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock idempotenceClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public FingerprintService fingerprintService(ObjectMapper objectMapper) {
        return new DefaultFingerprintService(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotenceService idempotenceService(IdempotenceStore store, Clock clock, IdempotenceProperties properties) {
        return new IdempotenceService(store, clock, properties.getTtl());
    }
}
