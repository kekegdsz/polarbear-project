package com.undersky.api.springbootserverapi.config;

import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 初始化管理员账号
 */
@Configuration
public class AdminInitConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminInitConfig.class);

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CommandLineRunner initAdminUser(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        return args -> {
            User existing = userMapper.selectByUsername(DEFAULT_ADMIN_USERNAME);
            if (existing != null) {
                // 已存在管理员或同名账号，跳过
                return;
            }

            User admin = new User();
            admin.setUsername(DEFAULT_ADMIN_USERNAME);
            admin.setNickname(DEFAULT_ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            admin.setRole("admin");
            admin.setVip(User.VIP_NORMAL);
            admin.setToken(UUID.randomUUID().toString().replace("-", ""));
            admin.setTokenExpireTime(LocalDateTime.now().plusDays(7));

            LocalDateTime now = LocalDateTime.now();
            admin.setCreatedAt(now);
            admin.setUpdatedAt(now);

            userMapper.insert(admin);
            log.info("Initialized default admin user. username='{}', password='{}'", DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
        };
    }
}

