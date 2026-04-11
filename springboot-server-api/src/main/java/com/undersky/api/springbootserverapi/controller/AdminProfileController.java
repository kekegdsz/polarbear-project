package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.exception.BusinessException;
import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.dto.ChangePasswordRequest;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.model.vo.AdminProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 管理后台 - 当前登录用户（个人信息、修改密码）
 */
@RestController
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminProfileController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取当前登录用户信息（用于左上角 "hello xxxx"）
     */
    @GetMapping
    public Result<AdminProfileVO> getProfile(HttpServletRequest request) {
        User user = (User) request.getAttribute("adminUser");
        AdminProfileVO vo = new AdminProfileVO();
        vo.setUsername(user.getUsername());
        return Result.success(vo);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordRequest req) {
        User user = (User) request.getAttribute("adminUser");

        if (!passwordEncoder.matches(req.getOldPassword().trim(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        String encoded = passwordEncoder.encode(req.getNewPassword().trim());
        userMapper.updatePassword(user.getId(), encoded, LocalDateTime.now());
        return Result.success();
    }
}
