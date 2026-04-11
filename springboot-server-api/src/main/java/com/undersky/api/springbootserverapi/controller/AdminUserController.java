package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.dto.PageResult;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.model.vo.UserSimpleVO;
import com.undersky.api.springbootserverapi.model.vo.UserStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台 - 用户相关接口
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserMapper userMapper;

    public AdminUserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户统计
     */
    @GetMapping("/stats")
    public Result<UserStatsVO> stats() {
        UserStatsVO vo = new UserStatsVO();
        vo.setTotalUsers(userMapper.countAll());
        vo.setTodayUsers(userMapper.countToday());
        vo.setVipUsers(userMapper.countVip());
        return Result.success(vo);
    }

    /**
     * 用户列表（简单分页）
     */
    @GetMapping
    public Result<PageResult<UserSimpleVO>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword) {
        if (page < 1) {
            page = 1;
        }
        if (size <= 0 || size > 100) {
            size = 10;
        }
        int offset = (page - 1) * size;

        long total = userMapper.countAll();
        List<User> users = userMapper.selectUsers(offset, size, keyword);
        List<UserSimpleVO> list = users.stream().map(this::toSimpleVO).collect(Collectors.toList());

        PageResult<UserSimpleVO> pageResult = new PageResult<>(total, list);
        return Result.success(pageResult);
    }

    private UserSimpleVO toSimpleVO(User user) {
        UserSimpleVO vo = new UserSimpleVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setMobile(user.getMobile());
        vo.setDeviceUuid(user.getDeviceUuid());
        vo.setVip(user.getVip());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}

