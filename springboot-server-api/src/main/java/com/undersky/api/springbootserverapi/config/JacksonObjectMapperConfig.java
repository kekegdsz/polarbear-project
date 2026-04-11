package com.undersky.api.springbootserverapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 + webmvc 场景下需显式注册 ObjectMapper，供 IM Netty 等组件注入。
 */
@Configuration
public class JacksonObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
