package com.undersky.api.springbootserverapi.im.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undersky.api.springbootserverapi.im.mapper.ImGroupMapper;
import com.undersky.api.springbootserverapi.im.mapper.ImGroupMemberMapper;
import com.undersky.api.springbootserverapi.im.mapper.ImMessageMapper;
import com.undersky.api.springbootserverapi.im.model.ImGroup;
import com.undersky.api.springbootserverapi.im.model.ImGroupMember;
import com.undersky.api.springbootserverapi.im.model.ImMessage;
import com.undersky.api.springbootserverapi.im.session.ImSessionManager;
import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.im.session.ImSessionEndpoint;
import com.undersky.api.springbootserverapi.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImChatService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ImMessageMapper messageMapper;
    private final ImGroupMapper groupMapper;
    private final ImGroupMemberMapper groupMemberMapper;
    private final UserMapper userMapper;
    private final ImSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public ImChatService(ImMessageMapper messageMapper,
                         ImGroupMapper groupMapper,
                         ImGroupMemberMapper groupMemberMapper,
                         UserMapper userMapper,
                         ImSessionManager sessionManager,
                         ObjectMapper objectMapper) {
        this.messageMapper = messageMapper;
        this.groupMapper = groupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.userMapper = userMapper;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    public User requireUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return u;
    }

    public void sendJson(ImSessionEndpoint endpoint, Map<String, Object> payload) {
        if (endpoint == null || !endpoint.isActive()) {
            return;
        }
        try {
            endpoint.sendJsonString(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            endpoint.close();
        }
    }

    @Transactional
    public Map<String, Object> createGroup(Long ownerUserId, String name) {
        requireUser(ownerUserId);
        ImGroup g = new ImGroup();
        g.setName(name == null || name.isBlank() ? "未命名群聊" : name.trim());
        g.setOwnerUserId(ownerUserId);
        groupMapper.insert(g);
        ImGroupMember m = new ImGroupMember();
        m.setGroupId(g.getId());
        m.setUserId(ownerUserId);
        groupMemberMapper.insert(m);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", "GROUP_CREATED");
        res.put("groupId", g.getId());
        res.put("name", g.getName());
        return res;
    }

    @Transactional
    public Map<String, Object> joinGroup(Long userId, Long groupId) {
        requireUser(userId);
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            throw new IllegalArgumentException("群不存在");
        }
        if (groupMemberMapper.countMember(groupId, userId) > 0) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("type", "GROUP_JOIN_OK");
            res.put("groupId", groupId);
            res.put("message", "已在群内");
            return res;
        }
        ImGroupMember m = new ImGroupMember();
        m.setGroupId(groupId);
        m.setUserId(userId);
        groupMemberMapper.insert(m);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", "GROUP_JOIN_OK");
        res.put("groupId", groupId);
        broadcastGroupSystem(groupId, userId, "用户 " + userId + " 加入群聊");
        return res;
    }

    private void broadcastGroupSystem(Long groupId, Long excludeUserId, String text) {
        List<Long> members = groupMapper.listMemberUserIds(groupId);
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("type", "GROUP_SYSTEM");
        evt.put("groupId", groupId);
        evt.put("body", text);
        for (Long uid : members) {
            if (uid.equals(excludeUserId)) {
                continue;
            }
            ImSessionEndpoint ep = sessionManager.endpointOf(uid);
            sendJson(ep, evt);
        }
    }

    @Transactional
    public ImMessage sendPrivate(Long fromUserId, Long toUserId, String body) {
        requireUser(fromUserId);
        requireUser(toUserId);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        ImMessage msg = new ImMessage();
        msg.setMsgType(ImMessage.TYPE_P2P);
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setBody(body.trim());
        messageMapper.insert(msg);
        return msg;
    }

    @Transactional
    public ImMessage sendGroup(Long fromUserId, Long groupId, String body) {
        requireUser(fromUserId);
        if (groupMemberMapper.countMember(groupId, fromUserId) == 0) {
            throw new IllegalArgumentException("不在该群内");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        ImMessage msg = new ImMessage();
        msg.setMsgType(ImMessage.TYPE_GROUP);
        msg.setFromUserId(fromUserId);
        msg.setGroupId(groupId);
        msg.setBody(body.trim());
        messageMapper.insert(msg);
        return msg;
    }

    public Map<String, Object> toPrivatePush(ImMessage m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "PRIVATE_MESSAGE");
        map.put("msgId", m.getId());
        map.put("fromUserId", m.getFromUserId());
        map.put("toUserId", m.getToUserId());
        map.put("body", m.getBody());
        map.put("createdAt", m.getCreatedAt() != null ? ISO.format(m.getCreatedAt()) : null);
        return map;
    }

    public Map<String, Object> toGroupPush(ImMessage m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "GROUP_MESSAGE");
        map.put("msgId", m.getId());
        map.put("groupId", m.getGroupId());
        map.put("fromUserId", m.getFromUserId());
        map.put("body", m.getBody());
        map.put("createdAt", m.getCreatedAt() != null ? ISO.format(m.getCreatedAt()) : null);
        return map;
    }

    public void pushPrivateToPeer(ImMessage msg) {
        ImSessionEndpoint peer = sessionManager.endpointOf(msg.getToUserId());
        sendJson(peer, toPrivatePush(msg));
    }

    public void broadcastGroupMessage(ImMessage msg) {
        List<Long> members = groupMapper.listMemberUserIds(msg.getGroupId());
        Map<String, Object> payload = toGroupPush(msg);
        for (Long uid : members) {
            if (uid.equals(msg.getFromUserId())) {
                continue;
            }
            sendJson(sessionManager.endpointOf(uid), payload);
        }
    }

    public Map<String, Object> historyP2P(Long viewerId, Long peerId, Long beforeId, int limit) {
        List<ImMessage> list = messageMapper.listP2PHistory(viewerId, peerId, beforeId, Math.min(limit, 200));
        return historyResult(list);
    }

    public Map<String, Object> historyGroup(Long viewerId, Long groupId, Long beforeId, int limit) {
        if (groupMemberMapper.countMember(groupId, viewerId) == 0) {
            throw new IllegalArgumentException("不在该群内");
        }
        List<ImMessage> list = messageMapper.listGroupHistory(groupId, beforeId, Math.min(limit, 200));
        return historyResult(list);
    }

    private Map<String, Object> historyResult(List<ImMessage> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            rows.add(messageRow(list.get(i)));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", "HISTORY_RESULT");
        res.put("messages", rows);
        return res;
    }

    private Map<String, Object> messageRow(ImMessage m) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("msgId", m.getId());
        row.put("msgType", m.getMsgType());
        row.put("fromUserId", m.getFromUserId());
        row.put("toUserId", m.getToUserId());
        row.put("groupId", m.getGroupId());
        row.put("body", m.getBody());
        row.put("createdAt", m.getCreatedAt() != null ? ISO.format(m.getCreatedAt()) : null);
        return row;
    }

    public Map<String, Object> conversations(Long userId) {
        List<ImMessage> p2p = messageMapper.listRecentP2PPeers(userId);
        List<ImMessage> grp = messageMapper.listRecentGroupSummaries(userId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (ImMessage m : p2p) {
            long peer = m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId();
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("convType", "P2P");
            it.put("peerUserId", peer);
            it.put("lastMessage", messageRow(m));
            items.add(it);
        }
        for (ImMessage m : grp) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("convType", "GROUP");
            it.put("groupId", m.getGroupId());
            it.put("lastMessage", messageRow(m));
            items.add(it);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", "CONVERSATIONS_RESULT");
        res.put("items", items);
        return res;
    }

    public Map<String, Object> userInfo(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("type", "ERROR");
            err.put("message", "用户不存在");
            return err;
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", "USER_INFO_RESULT");
        res.put("userId", u.getId());
        res.put("username", u.getUsername());
        res.put("mobile", u.getMobile());
        res.put("role", u.getRole());
        res.put("vip", u.getVip());
        return res;
    }

    public Map<String, Object> groupInfo(Long groupId) {
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("type", "ERROR");
            err.put("message", "群不存在");
            return err;
        }
        List<Long> memberIds = groupMapper.listMemberUserIds(groupId);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", "GROUP_INFO_RESULT");
        res.put("groupId", g.getId());
        res.put("name", g.getName());
        res.put("ownerUserId", g.getOwnerUserId());
        res.put("memberIds", memberIds);
        res.put("memberCount", memberIds.size());
        return res;
    }
}
