package com.undersky.api.springbootserverapi.config;

import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简单基于 token 的管理员鉴权拦截器
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    public AdminAuthInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        User user = userMapper.selectByToken(token);
        if (user == null || !"admin".equals(user.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        request.setAttribute("adminUser", user);
        return true;
    }
}

