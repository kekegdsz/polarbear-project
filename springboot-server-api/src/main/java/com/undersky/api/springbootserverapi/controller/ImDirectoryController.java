package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.model.vo.DirectoryUserVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * IM 客户端：已登录用户可拉取用户目录（用于通讯录）
 */
@RestController
@RequestMapping("/im/directory")
public class ImDirectoryController {

    private final UserMapper userMapper;

    public ImDirectoryController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户列表（需携带登录 token，非管理员也可用）
     */
    @GetMapping("/users")
    public Result<List<DirectoryUserVO>> listUsers(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestParam(defaultValue = "2000") int limit,
            HttpServletResponse response) {
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("请先登录");
        }
        User me = userMapper.selectByToken(token);
        if (me == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.error("登录已失效");
        }
        int cap = Math.min(Math.max(limit, 1), 2000);
        List<User> users = userMapper.selectUsers(0, cap, null);
        List<DirectoryUserVO> list = users.stream()
                .map(this::toDirectoryVo)
                .collect(Collectors.toList());
        return Result.success(list);
    }

    private DirectoryUserVO toDirectoryVo(User user) {
        DirectoryUserVO vo = new DirectoryUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setMobile(user.getMobile());
        return vo;
    }
}
