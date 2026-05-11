package com.idempotence.idempotencelib.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idempotence.idempotencelib.core.store.IdempotenceStore;
import com.idempotence.idempotencelib.redis.RedisIdempotenceMapper;
import com.idempotence.idempotencelib.redis.RedisIdempotenceStore;
import com.idempotence.idempotencelib.spring.properties.IdempotenceProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

@AutoConfiguration(after = {IdempotenceAutoConfiguration.class, RedisAutoConfiguration.class})
@ConditionalOnClass(StringRedisTemplate.class)
public class IdempotenceRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisIdempotenceMapper redisIdempotenceMapper() {
        return new RedisIdempotenceMapper();
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(IdempotenceStore.class)
    public IdempotenceStore idempotenceStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                              RedisIdempotenceMapper mapper, IdempotenceProperties properties) {
        return new RedisIdempotenceStore(redisTemplate, objectMapper, mapper,
                properties.getKeyPrefix(), properties.getRedisFailurePolicy());
    }
}
