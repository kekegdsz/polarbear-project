package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.exception.BusinessException;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.dto.UpdateProfileRequest;
import com.undersky.api.springbootserverapi.model.vo.LoginVO;
import com.undersky.api.springbootserverapi.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户资料（非管理后台，使用与普通 IM 相同的 X-Auth-Token）
 */
@RestController
@RequestMapping("/user")
public class UserProfileController {

    private final AuthService authService;

    public UserProfileController(AuthService authService) {
        this.authService = authService;
    }

    @PutMapping("/profile")
    public Result<LoginVO> updateProfile(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletResponse response) {
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("请先登录");
        }
        try {
            LoginVO vo = authService.updateProfile(token.trim(), request.getNickname());
            return Result.success(vo);
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        }
    }
}
