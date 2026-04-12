package com.undersky.api.springbootserverapi.config;

import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.entity.User;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 启动时确保 IM 系统发信用户存在于 users 表（群广播 / 系统私聊 from_user_id）。
 */
@Component
public class ImSystemUserProvisioner implements InitializingBean {

    @Value("${im.system-sender-user-id:999}")
    private long systemUserId;

    private final UserMapper userMapper;

    public ImSystemUserProvisioner(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void afterPropertiesSet() {
        if (systemUserId <= 0) {
            return;
        }
        if (userMapper.selectById(systemUserId) != null) {
            return;
        }
        User u = new User();
        u.setId(systemUserId);
        u.setDeviceUuid("im-system");
        u.setUsername("im_system");
        u.setNickname("系统通知");
        u.setPassword("!");
        u.setMobile(null);
        u.setRole("admin");
        userMapper.insertWithId(u);
    }
}
