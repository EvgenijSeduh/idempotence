package com.idempotence.idempotencelib.spring.web;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

public class IdempotenceWebMvcConfigurer implements WebMvcConfigurer {

    private final IdempotenceInterceptor idempotenceInterceptor;

    public IdempotenceWebMvcConfigurer(IdempotenceInterceptor idempotenceInterceptor) {
        this.idempotenceInterceptor = Objects.requireNonNull(idempotenceInterceptor);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotenceInterceptor);
    }
}
