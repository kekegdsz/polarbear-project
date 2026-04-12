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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    /** 后台向群/用户发系统消息时使用的发送方用户 ID（需在 users 表存在） */
    @Value("${im.system-sender-user-id:999}")
    private long systemSenderUserId;

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

    /** 当前是否有有效 IM 连接（Netty / Spring WS 任一在线） */
    public boolean isUserOnline(long userId) {
        ImSessionEndpoint ep = sessionManager.endpointOf(userId);
        return ep != null && ep.isActive();
    }

    /** 广播用户上下线（除本人外） */
    public void broadcastPresence(long userId, boolean online) {
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("type", "PRESENCE");
        evt.put("userId", userId);
        evt.put("online", online);
        sessionManager.forEachOnline((uid, endpoint) -> {
            if (!uid.equals(userId)) {
                sendJson(endpoint, evt);
            }
        });
    }

    /**
     * 新连接认证成功后，向该连接推送当前其他在线用户的 PRESENCE，便于客户端同步列表状态。
     */
    public void sendPresenceSnapshotTo(ImSessionEndpoint newEndpoint, long newUserId) {
        sessionManager.forEachOnline((uid, endpoint) -> {
            if (uid.equals(newUserId)) {
                return;
            }
            Map<String, Object> evt = new LinkedHashMap<>();
            evt.put("type", "PRESENCE");
            evt.put("userId", uid);
            evt.put("online", true);
            sendJson(newEndpoint, evt);
        });
    }

    @Transactional
    public Map<String, Object> createGroup(Long ownerUserId, String requestedName, List<Long> otherMemberIds) {
        requireUser(ownerUserId);
        LinkedHashSet<Long> others = new LinkedHashSet<>();
        if (otherMemberIds != null) {
            for (Long id : otherMemberIds) {
                if (id == null || id.equals(ownerUserId)) {
                    continue;
                }
                requireUser(id);
                others.add(id);
            }
        }
        String name;
        if (others.isEmpty()) {
            name = (requestedName == null || requestedName.isBlank())
                    ? "未命名群聊"
                    : truncateGroupName(requestedName.trim());
        } else {
            name = (requestedName == null || requestedName.isBlank())
                    ? defaultGroupNameFromMembers(ownerUserId, new ArrayList<>(others))
                    : truncateGroupName(requestedName.trim());
        }
        ImGroup g = new ImGroup();
        g.setName(name);
        g.setOwnerUserId(ownerUserId);
        groupMapper.insert(g);
        long gid = g.getId();
        ImGroupMember ownerRow = new ImGroupMember();
        ownerRow.setGroupId(gid);
        ownerRow.setUserId(ownerUserId);
        ownerRow.setRole(ImGroupMember.ROLE_OWNER);
        groupMemberMapper.insert(ownerRow);
        for (Long uid : others) {
            ImGroupMember mm = new ImGroupMember();
            mm.setGroupId(gid);
            mm.setUserId(uid);
            mm.setRole(ImGroupMember.ROLE_MEMBER);
            groupMemberMapper.insert(mm);
        }
        // 会话列表依赖 im_messages 中该群的最近一条消息；新建群尚无消息时不会出现在 CONVERSATIONS 中
        ImMessage welcome = new ImMessage();
        welcome.setMsgType(ImMessage.TYPE_GROUP);
        welcome.setFromUserId(ownerUserId);
        welcome.setGroupId(gid);
        welcome.setBody("群聊已创建");
        messageMapper.insert(welcome);
        broadcastGroupMessage(welcome);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", "GROUP_CREATED");
        res.put("groupId", gid);
        res.put("name", g.getName());
        return res;
    }

    private String defaultGroupNameFromMembers(Long ownerUserId, List<Long> othersInOrder) {
        List<Long> firstThree = new ArrayList<>();
        firstThree.add(ownerUserId);
        for (Long id : othersInOrder) {
            if (firstThree.size() >= 3) {
                break;
            }
            firstThree.add(id);
        }
        List<User> loaded = userMapper.selectByIds(firstThree);
        Map<Long, User> byId = new HashMap<>();
        for (User u : loaded) {
            byId.put(u.getId(), u);
        }
        StringBuilder sb = new StringBuilder();
        for (Long id : firstThree) {
            User u = byId.get(id);
            if (u == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(displayName(u));
        }
        String s = sb.toString();
        return s.isEmpty() ? "未命名群聊" : truncateGroupName(s);
    }

    private static String displayName(User u) {
        if (u.getNickname() != null && !u.getNickname().isBlank()) {
            return u.getNickname().trim();
        }
        if (u.getUsername() != null && !u.getUsername().isBlank()) {
            return u.getUsername().trim();
        }
        return "用户" + u.getId();
    }

    private static String truncateGroupName(String s) {
        if (s.length() <= 128) {
            return s;
        }
        return s.substring(0, 128);
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
        m.setRole(ImGroupMember.ROLE_MEMBER);
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

    /**
     * 管理后台：系统账号向指定用户发送一条私聊（发送方不必与对方有会话关系）。
     */
    @Transactional
    public ImMessage adminSystemPrivateToUser(long toUserId, String body) {
        if (toUserId == systemSenderUserId) {
            throw new IllegalArgumentException("不能向系统账号自身发信");
        }
        requireUser(systemSenderUserId);
        requireUser(toUserId);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        ImMessage msg = new ImMessage();
        msg.setMsgType(ImMessage.TYPE_P2P);
        msg.setFromUserId(systemSenderUserId);
        msg.setToUserId(toUserId);
        msg.setBody(body.trim());
        messageMapper.insert(msg);
        pushPrivateToPeer(msg);
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

    public Map<String, Object> historyP2P(Long viewerId, Long peerId, Long beforeId, Long afterId, int limit) {
        if (beforeId != null && afterId != null) {
            throw new IllegalArgumentException("beforeId 与 afterId 不能同时指定");
        }
        int lim = Math.min(limit, 200);
        if (afterId != null) {
            List<ImMessage> list = messageMapper.listP2PHistoryAfter(viewerId, peerId, afterId, lim);
            return historyResultAscending(list);
        }
        List<ImMessage> list = messageMapper.listP2PHistory(viewerId, peerId, beforeId, lim);
        return historyResult(list);
    }

    public Map<String, Object> historyGroup(Long viewerId, Long groupId, Long beforeId, Long afterId, int limit) {
        if (groupMemberMapper.countMember(groupId, viewerId) == 0) {
            throw new IllegalArgumentException("不在该群内");
        }
        if (beforeId != null && afterId != null) {
            throw new IllegalArgumentException("beforeId 与 afterId 不能同时指定");
        }
        int lim = Math.min(limit, 200);
        if (afterId != null) {
            List<ImMessage> list = messageMapper.listGroupHistoryAfter(groupId, afterId, lim);
            return historyResultAscending(list);
        }
        List<ImMessage> list = messageMapper.listGroupHistory(groupId, beforeId, lim);
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

    /** 列表已按 id 升序（增量同步） */
    private Map<String, Object> historyResultAscending(List<ImMessage> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ImMessage m : list) {
            rows.add(messageRow(m));
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
            ImGroup gg = groupMapper.findById(m.getGroupId());
            if (gg != null) {
                it.put("groupName", gg.getName());
            }
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
        res.put("nickname", u.getNickname());
        res.put("mobile", u.getMobile());
        res.put("role", u.getRole());
        res.put("vip", u.getVip());
        res.put("online", isUserOnline(u.getId()));
        return res;
    }

    public Map<String, Object> groupInfo(Long groupId, Long viewerUserId) {
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("type", "ERROR");
            err.put("message", "群不存在");
            return err;
        }
        if (groupMemberMapper.countMember(groupId, viewerUserId) == 0) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("type", "ERROR");
            err.put("message", "不在该群内");
            return err;
        }
        List<ImGroupMember> rows = groupMemberMapper.listByGroupId(groupId);
        List<Long> memberIds = new ArrayList<>();
        List<Map<String, Object>> members = new ArrayList<>();
        for (ImGroupMember row : rows) {
            memberIds.add(row.getUserId());
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("userId", row.getUserId());
            mm.put("role", row.getRole());
            members.add(mm);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", "GROUP_INFO_RESULT");
        res.put("groupId", g.getId());
        res.put("name", g.getName());
        res.put("ownerUserId", g.getOwnerUserId());
        res.put("memberIds", memberIds);
        res.put("memberCount", memberIds.size());
        res.put("members", members);
        return res;
    }

    @Transactional
    public Map<String, Object> renameGroup(Long actorUserId, Long groupId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("群名不能为空");
        }
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            throw new IllegalArgumentException("群不存在");
        }
        if (groupMemberMapper.countMember(groupId, actorUserId) == 0) {
            throw new IllegalArgumentException("不在该群内");
        }
        String role = groupMemberMapper.selectRole(groupId, actorUserId);
        if (!ImGroupMember.ROLE_OWNER.equals(role) && !ImGroupMember.ROLE_ADMIN.equals(role)) {
            throw new IllegalArgumentException("仅群主或管理员可修改群名");
        }
        groupMapper.updateName(groupId, truncateGroupName(newName.trim()));
        return groupInfo(groupId, actorUserId);
    }

    @Transactional
    public Map<String, Object> setGroupAdmin(Long actorUserId, Long groupId, Long targetUserId) {
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            throw new IllegalArgumentException("群不存在");
        }
        if (!actorUserId.equals(g.getOwnerUserId())) {
            throw new IllegalArgumentException("仅群主可任命管理员");
        }
        if (targetUserId.equals(g.getOwnerUserId())) {
            throw new IllegalArgumentException("不能修改群主角色");
        }
        if (groupMemberMapper.countMember(groupId, targetUserId) == 0) {
            throw new IllegalArgumentException("目标用户不在群内");
        }
        groupMemberMapper.updateRole(groupId, targetUserId, ImGroupMember.ROLE_ADMIN);
        return groupInfo(groupId, actorUserId);
    }

    @Transactional
    public Map<String, Object> removeGroupAdmin(Long actorUserId, Long groupId, Long targetUserId) {
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            throw new IllegalArgumentException("群不存在");
        }
        if (!actorUserId.equals(g.getOwnerUserId())) {
            throw new IllegalArgumentException("仅群主可取消管理员");
        }
        if (targetUserId.equals(g.getOwnerUserId())) {
            throw new IllegalArgumentException("不能修改群主角色");
        }
        String r = groupMemberMapper.selectRole(groupId, targetUserId);
        if (!ImGroupMember.ROLE_ADMIN.equals(r)) {
            throw new IllegalArgumentException("该成员不是管理员");
        }
        groupMemberMapper.updateRole(groupId, targetUserId, ImGroupMember.ROLE_MEMBER);
        return groupInfo(groupId, actorUserId);
    }

    /**
     * 管理后台：以系统账号向群内广播一条群消息（发送方不必在群内）。
     */
    @Transactional
    public ImMessage adminBroadcastGroup(long groupId, String body) {
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            throw new IllegalArgumentException("群不存在");
        }
        requireUser(systemSenderUserId);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        ImMessage msg = new ImMessage();
        msg.setMsgType(ImMessage.TYPE_GROUP);
        msg.setFromUserId(systemSenderUserId);
        msg.setGroupId(groupId);
        msg.setBody(body.trim());
        messageMapper.insert(msg);
        broadcastGroupMessage(msg);
        return msg;
    }

    /**
     * 管理后台：系统账号向群内指定成员各发一条私聊（校验成员在群内）。
     */
    @Transactional
    public int adminPrivateNotifyGroupMembers(long groupId, List<Long> userIds, String body) {
        ImGroup g = groupMapper.findById(groupId);
        if (g == null) {
            throw new IllegalArgumentException("群不存在");
        }
        requireUser(systemSenderUserId);
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("请指定用户");
        }
        String text = body.trim();
        int n = 0;
        for (Long uid : userIds) {
            if (uid == null) {
                continue;
            }
            if (groupMemberMapper.countMember(groupId, uid) == 0) {
                throw new IllegalArgumentException("用户不在该群内: " + uid);
            }
            requireUser(uid);
            ImMessage msg = new ImMessage();
            msg.setMsgType(ImMessage.TYPE_P2P);
            msg.setFromUserId(systemSenderUserId);
            msg.setToUserId(uid);
            msg.setBody(text);
            messageMapper.insert(msg);
            pushPrivateToPeer(msg);
            n++;
        }
        return n;
    }
}
