package com.undersky.api.springbootserverapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot Server API")
                        .version("1.0.0")
                        .description("REST API 接口文档，支持设备 UUID 自动注册、账号密码注册登录")
                        .contact(new Contact().name("API")));
    }
}
