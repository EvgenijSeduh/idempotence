package com.idempotence.idempotencelib.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.fingerprint.FingerprintService;
import com.idempotence.idempotencelib.core.service.IdempotenceService;
import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import com.idempotence.idempotencelib.spring.web.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(after = {IdempotenceAutoConfiguration.class, IdempotenceRedisAutoConfiguration.class, WebMvcAutoConfiguration.class})
@ConditionalOnClass(DispatcherServlet.class)
public class IdempotenceWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyKeyResolver idempotencyKeyResolver(IdempotenceProperties properties) {
        return new DefaultIdempotencyKeyResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpRequestFingerprintBuilder httpRequestFingerprintBuilder(FingerprintService fingerprintService) {
        return new HttpRequestFingerprintBuilder(fingerprintService);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotenceInterceptor idempotenceInterceptor(IdempotenceService idempotenceService,
                                                          IdempotencyKeyResolver keyResolver,
                                                          HttpRequestFingerprintBuilder fingerprintBuilder,
                                                          IdempotenceProperties properties, ObjectMapper objectMapper) {
        return new IdempotenceInterceptor(idempotenceService, keyResolver, fingerprintBuilder, properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotenceResponseAdvice idempotenceResponseAdvice(IdempotenceService idempotenceService,
                                                                ObjectMapper objectMapper, IdempotenceProperties properties) {
        return new IdempotenceResponseAdvice(idempotenceService, objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "idempotenceWebMvcConfigurer")
    public WebMvcConfigurer idempotenceWebMvcConfigurer(IdempotenceInterceptor interceptor) {
        return new IdempotenceWebMvcConfigurer(interceptor);
    }

    @Bean
    @ConditionalOnMissingBean(name = "idempotenceCachedBodyFilter")
    public FilterRegistrationBean<CachedBodyFilter> idempotenceCachedBodyFilter(IdempotenceProperties properties) {
        FilterRegistrationBean<CachedBodyFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new CachedBodyFilter(properties));
        bean.setOrder(Integer.MIN_VALUE);
        return bean;
    }
}
