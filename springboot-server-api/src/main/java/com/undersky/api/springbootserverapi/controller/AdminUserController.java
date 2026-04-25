package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.im.service.ImChatService;
import com.undersky.api.springbootserverapi.im.session.ImSessionManager;
import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.im.model.ImMessage;
import com.undersky.api.springbootserverapi.model.dto.AdminSystemP2pRequest;
import com.undersky.api.springbootserverapi.model.dto.PageResult;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.model.vo.UserAdminFullVO;
import com.undersky.api.springbootserverapi.model.vo.UserStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台 - 用户相关接口
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserMapper userMapper;
    private final ImChatService imChatService;
    private final ImSessionManager imSessionManager;

    public AdminUserController(UserMapper userMapper,
                               ImChatService imChatService,
                               ImSessionManager imSessionManager) {
        this.userMapper = userMapper;
        this.imChatService = imChatService;
        this.imSessionManager = imSessionManager;
    }

    @GetMapping("/stats")
    public Result<UserStatsVO> stats() {
        UserStatsVO vo = new UserStatsVO();
        vo.setTotalUsers(userMapper.countAllExcludingImSystem());
        vo.setTodayUsers(userMapper.countToday());
        vo.setVipUsers(userMapper.countVip());
        vo.setOnlineImUsers(imSessionManager.onlineSessionCount());
        return Result.success(vo);
    }

    @GetMapping
    public Result<PageResult<UserAdminFullVO>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) String keyword) {
        if (page < 1) {
            page = 1;
        }
        if (size <= 0 || size > 100) {
            size = 10;
        }
        int offset = (page - 1) * size;

        long total = userMapper.countUsers(keyword);
        List<User> users = userMapper.selectUsers(offset, size, keyword);
        List<UserAdminFullVO> list = users.stream().map(this::toFullVO).collect(Collectors.toList());

        PageResult<UserAdminFullVO> pageResult = new PageResult<>(total, list);
        return Result.success(pageResult);
    }

    /**
     * 管理后台：以系统账号向指定用户发送一条 P2P 私聊（用户列表「系统私聊」入口）。
     */
    @PostMapping("/{userId}/system-p2p")
    public Result<Map<String, Object>> systemPrivateP2p(@PathVariable("userId") long userId,
                                                        @RequestBody(required = false) AdminSystemP2pRequest req) {
        try {
            String body = req != null ? req.getBody() : null;
            ImMessage m = imChatService.adminSystemPrivateToUser(userId, body);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("msgId", m.getId());
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户（管理后台操作）
     */
    @PostMapping("/{userId}/delete")
    public Result<Void> delete(@PathVariable("userId") long userId) {
        if (userId == 999) {
            return Result.error("系统账号不可删除");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return Result.success();
        }
        if ("admin".equals(u.getRole())) {
            return Result.error("管理员不可删除");
        }
        userMapper.deleteById(userId);
        return Result.success();
    }

    private UserAdminFullVO toFullVO(User user) {
        UserAdminFullVO vo = new UserAdminFullVO();
        vo.setId(user.getId());
        vo.setDeviceUuid(user.getDeviceUuid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setMobile(user.getMobile());
        String tok = user.getToken();
        vo.setHasToken(tok != null && !tok.isBlank());
        vo.setTokenMasked(maskToken(tok));
        vo.setTokenExpireTime(user.getTokenExpireTime());
        vo.setVip(user.getVip());
        vo.setVipExpireTime(user.getVipExpireTime());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        vo.setOnline(imChatService.isUserOnline(user.getId()));
        return vo;
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        int n = token.length();
        if (n <= 6) {
            return "******";
        }
        return "…" + token.substring(n - 4);
    }
}
