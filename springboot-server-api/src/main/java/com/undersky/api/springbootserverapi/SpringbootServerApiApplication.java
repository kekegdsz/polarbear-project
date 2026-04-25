package com.undersky.api.springbootserverapi;

import com.undersky.api.springbootserverapi.im.config.ImNettyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ImNettyProperties.class)
public class SpringbootServerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootServerApiApplication.class, args);
    }

}
