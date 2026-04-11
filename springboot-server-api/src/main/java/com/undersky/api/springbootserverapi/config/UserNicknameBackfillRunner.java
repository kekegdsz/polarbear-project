package com.undersky.api.springbootserverapi.config;

import com.undersky.api.springbootserverapi.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 为缺少昵称的用户补齐 nickname（用户名或「用户」+id）
 */
@Configuration
public class UserNicknameBackfillRunner {

    private static final Logger log = LoggerFactory.getLogger(UserNicknameBackfillRunner.class);

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public CommandLineRunner backfillUserNicknames(UserMapper userMapper) {
        return args -> {
            int n = userMapper.backfillBlankNicknames();
            if (n > 0) {
                log.info("Backfilled nickname for {} user row(s)", n);
            }
        };
    }
}
