package com.undersky.api.springbootserverapi.service;

import com.undersky.api.springbootserverapi.exception.BusinessException;
import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.dto.LoginRequest;
import com.undersky.api.springbootserverapi.model.dto.RegisterRequest;
import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.model.vo.LoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 认证服务（支持设备 UUID 自动注册、账号密码注册）
 */
@Service
public class AuthService {

    private static final int TOKEN_EXPIRE_DAYS = 7;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginVO register(RegisterRequest request) {
        String deviceUuid = trimToNull(request.getDeviceUuid());
        String username = trimToNull(request.getUsername());
        String password = trimToNull(request.getPassword());
        String mobile = trimToNull(request.getMobile());

        // 校验：至少 deviceUuid 或 (username+password)
        boolean hasDeviceUuid = StringUtils.hasText(deviceUuid);
        boolean hasAccount = StringUtils.hasText(username) && StringUtils.hasText(password);

        if (!hasDeviceUuid && !hasAccount) {
            throw new BusinessException("请提供设备 UUID 或 用户名+密码");
        }
        if (hasAccount && (username.length() < 3 || password.length() < 6)) {
            throw new BusinessException("用户名至少3位、密码至少6位");
        }

        // 方式一：仅设备 UUID 自动注册
        if (hasDeviceUuid && !hasAccount) {
            User user = userMapper.selectByDeviceUuid(deviceUuid);
            if (user != null) {
                refreshToken(user);
                return toLoginVO(userMapper.selectById(user.getId()));
            }
            user = createDeviceUser(deviceUuid, mobile);
            userMapper.insert(user);
            ensureNicknameAfterInsert(user);
            return toLoginVO(userMapper.selectById(user.getId()));
        }

        // 方式二：账号密码注册（可绑定 deviceUuid）
        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (StringUtils.hasText(mobile) && userMapper.countByMobile(mobile) > 0) {
            throw new BusinessException("手机号已注册");
        }

        User user = new User();
        user.setDeviceUuid(deviceUuid);
        user.setUsername(username);
        user.setNickname(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setMobile(mobile);
        user.setToken(UUID.randomUUID().toString().replace("-", ""));
        user.setTokenExpireTime(LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS));
        user.setVip(User.VIP_NORMAL);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userMapper.insert(user);
        return toLoginVO(userMapper.selectById(user.getId()));
    }

    private User createDeviceUser(String deviceUuid, String mobile) {
        User user = new User();
        user.setDeviceUuid(deviceUuid);
        user.setMobile(mobile);
        user.setToken(UUID.randomUUID().toString().replace("-", ""));
        user.setTokenExpireTime(LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS));
        user.setVip(User.VIP_NORMAL);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    private void refreshToken(User user) {
        user.setToken(UUID.randomUUID().toString().replace("-", ""));
        user.setTokenExpireTime(LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getPassword() == null) {
            throw new BusinessException("该账号未设置密码，请使用设备登录");
        }

        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        refreshToken(user);
        return toLoginVO(userMapper.selectById(user.getId()));
    }

    @Transactional
    public LoginVO updateProfile(String token, String nicknameRaw) {
        User user = userMapper.selectByToken(token);
        if (user == null) {
            throw new BusinessException("登录已失效");
        }
        String nick = nicknameRaw != null ? nicknameRaw.trim() : "";
        if (nick.isEmpty()) {
            throw new BusinessException("昵称不能为空");
        }
        if (nick.length() > 32) {
            throw new BusinessException("昵称最多32个字符");
        }
        user.setNickname(nick);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toLoginVO(userMapper.selectById(user.getId()));
    }

    private void ensureNicknameAfterInsert(User user) {
        if (StringUtils.hasText(user.getNickname())) {
            return;
        }
        String nick = StringUtils.hasText(user.getUsername())
                ? user.getUsername()
                : ("用户" + user.getId());
        user.setNickname(nick);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private String trimToNull(String s) {
        return s != null && !s.isBlank() ? s.trim() : null;
    }

    private LoginVO toLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setUserId(String.valueOf(user.getId()));
        vo.setNickname(user.getNickname() != null ? user.getNickname() : "");
        vo.setMobile(user.getMobile() != null ? user.getMobile() : "");
        vo.setVipFlag(User.VIP_PAID.equals(user.getVip()));
        vo.setVip(user.getVip() != null ? user.getVip() : User.VIP_NORMAL);
        vo.setVipFormat(User.VIP_PAID.equals(user.getVip()) ? "付费用户" : "普通用户");
        vo.setVipExpireTime(user.getVipExpireTime() != null
                ? user.getVipExpireTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : 0L);
        vo.setToken(user.getToken());
        vo.setRole(user.getRole() != null ? user.getRole() : "user");
        vo.setRegisterTime(user.getCreatedAt() != null
                ? user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : 0L);
        vo.setResponseTime(System.currentTimeMillis());
        return vo;
    }
}
