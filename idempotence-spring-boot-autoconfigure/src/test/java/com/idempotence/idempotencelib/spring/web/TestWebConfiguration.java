package com.idempotence.idempotencelib.spring.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.idempotence.idempotencelib.autoconfigure.IdempotenceAutoConfiguration;
import com.idempotence.idempotencelib.autoconfigure.IdempotenceRedisAutoConfiguration;
import com.idempotence.idempotencelib.autoconfigure.IdempotenceWebAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration
@ImportAutoConfiguration({
        IdempotenceAutoConfiguration.class,
        IdempotenceRedisAutoConfiguration.class,
        IdempotenceWebAutoConfiguration.class
})
public class TestWebConfiguration {

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        String host = System.getProperty("redis.host", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("redis.port", "6379"));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public TestOrderController testOrderController() {
        return new TestOrderController();
    }
}
