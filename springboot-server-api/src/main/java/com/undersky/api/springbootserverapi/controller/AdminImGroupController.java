package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.im.mapper.ImGroupMapper;
import com.undersky.api.springbootserverapi.im.mapper.ImGroupMemberMapper;
import com.undersky.api.springbootserverapi.im.model.ImGroup;
import com.undersky.api.springbootserverapi.im.model.ImGroupMember;
import com.undersky.api.springbootserverapi.im.model.ImMessage;
import com.undersky.api.springbootserverapi.im.service.ImChatService;
import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.dto.AdminImBroadcastRequest;
import com.undersky.api.springbootserverapi.model.dto.AdminImNotifyMembersRequest;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.model.vo.ImGroupAdminDetailVO;
import com.undersky.api.springbootserverapi.model.vo.ImGroupAdminListVO;
import com.undersky.api.springbootserverapi.model.vo.ImGroupMemberAdminVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台 - IM 群管理与系统发信
 */
@RestController
@RequestMapping("/admin/im/groups")
public class AdminImGroupController {

    private final ImGroupMapper groupMapper;
    private final ImGroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;
    private final ImChatService imChatService;

    public AdminImGroupController(ImGroupMapper groupMapper,
                                  ImGroupMemberMapper groupMemberMapper,
                                  UserMapper userMapper,
                                  ImChatService imChatService) {
        this.groupMapper = groupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.userMapper = userMapper;
        this.imChatService = imChatService;
    }

    @GetMapping
    public Result<List<ImGroupAdminListVO>> list() {
        return Result.success(groupMapper.listAllForAdmin());
    }

    @GetMapping("/{groupId}")
    public Result<ImGroupAdminDetailVO> detail(@PathVariable("groupId") long groupId) {
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            return Result.error("群不存在");
        }
        List<ImGroupMember> rows = groupMemberMapper.listByGroupId(groupId);
        List<ImGroupMemberAdminVO> members = new ArrayList<>();
        for (ImGroupMember row : rows) {
            ImGroupMemberAdminVO v = new ImGroupMemberAdminVO();
            v.setUserId(row.getUserId());
            v.setRole(row.getRole());
            v.setJoinedAt(row.getJoinedAt());
            User u = userMapper.selectById(row.getUserId());
            if (u != null) {
                v.setUsername(u.getUsername());
                v.setNickname(u.getNickname());
                v.setMobile(u.getMobile());
            }
            v.setOnline(imChatService.isUserOnline(row.getUserId()));
            members.add(v);
        }
        ImGroupAdminDetailVO d = new ImGroupAdminDetailVO();
        d.setId(g.getId());
        d.setName(g.getName());
        d.setOwnerUserId(g.getOwnerUserId());
        d.setCreatedAt(g.getCreatedAt());
        d.setMemberCount(members.size());
        d.setMembers(members);
        return Result.success(d);
    }

    @PostMapping("/{groupId}/broadcast")
    public Result<Map<String, Object>> broadcast(@PathVariable("groupId") long groupId,
                                                 @RequestBody AdminImBroadcastRequest req) {
        try {
            String body = req != null ? req.getBody() : null;
            ImMessage m = imChatService.adminBroadcastGroup(groupId, body);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("msgId", m.getId());
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/notify-members")
    public Result<Map<String, Object>> notifyMembers(@PathVariable("groupId") long groupId,
                                                     @RequestBody AdminImNotifyMembersRequest req) {
        try {
            if (req == null) {
                return Result.error("请求体不能为空");
            }
            int n = imChatService.adminPrivateNotifyGroupMembers(groupId, req.getUserIds(), req.getBody());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("sentCount", n);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
