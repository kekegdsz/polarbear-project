package com.undersky.api.springbootserverapi.im;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undersky.api.springbootserverapi.im.model.ImMessage;
import com.undersky.api.springbootserverapi.im.service.ImChatService;
import com.undersky.api.springbootserverapi.im.session.ImSessionEndpoint;
import com.undersky.api.springbootserverapi.im.session.ImSessionManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Netty 与 Spring WebSocket 共用的 JSON 指令处理。
 */
@Component
public class ImJsonMessageProcessor {

    private final ImChatService chatService;
    private final ImSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public ImJsonMessageProcessor(ImChatService chatService,
                                  ImSessionManager sessionManager,
                                  ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    public void handleText(ImSessionEndpoint ep, String text) {
        Map<String, Object> msg;
        try {
            msg = objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            err(ep, "JSON 解析失败");
            return;
        }
        String type = msg.get("type") == null ? "" : String.valueOf(msg.get("type"));
        try {
            switch (type) {
                case "AUTH" -> handleAuth(ep, msg);
                case "PRIVATE_SEND" -> handlePrivateSend(ep, msg);
                case "GROUP_SEND" -> handleGroupSend(ep, msg);
                case "GROUP_CREATE" -> handleGroupCreate(ep, msg);
                case "GROUP_JOIN" -> handleGroupJoin(ep, msg);
                case "HISTORY" -> handleHistory(ep, msg);
                case "CONVERSATIONS" -> handleConversations(ep, msg);
                case "USER_INFO" -> handleUserInfo(ep, msg);
                case "GROUP_INFO" -> handleGroupInfo(ep, msg);
                case "GROUP_RENAME" -> handleGroupRename(ep, msg);
                case "GROUP_SET_ADMIN" -> handleGroupSetAdmin(ep, msg);
                case "GROUP_REMOVE_ADMIN" -> handleGroupRemoveAdmin(ep, msg);
                default -> err(ep, "未知指令: " + type);
            }
        } catch (IllegalArgumentException ex) {
            err(ep, ex.getMessage());
        } catch (Exception ex) {
            err(ep, "处理失败: " + ex.getMessage());
        }
    }

    private Long uid(ImSessionEndpoint ep) {
        return ep.getBoundUserId();
    }

    private void requireAuth(ImSessionEndpoint ep) {
        if (uid(ep) == null) {
            throw new IllegalArgumentException("请先发送 AUTH");
        }
    }

    private long longVal(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("缺少数值参数");
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(o));
    }

    /**
     * 客户端 body 应为 JSON 字符串；若 Jackson 将内层 JSON 解析成 Map，则再序列化回字符串，避免入库成 {@code {k=img}} 导致端上无法展示。
     */
    private String bodyToStoredString(Object bodyObj) {
        if (bodyObj == null) {
            return "";
        }
        if (bodyObj instanceof String s) {
            return s.trim();
        }
        try {
            return objectMapper.writeValueAsString(bodyObj);
        } catch (Exception e) {
            return String.valueOf(bodyObj);
        }
    }

    private void handleAuth(ImSessionEndpoint ep, Map<String, Object> msg) {
        long userId = longVal(msg.get("userId"));
        chatService.requireUser(userId);
        sessionManager.bind(userId, ep);
        chatService.broadcastPresence(userId, true);
        chatService.sendPresenceSnapshotTo(ep, userId);
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("type", "AUTH_OK");
        ok.put("userId", userId);
        chatService.sendJson(ep, ok);
    }

    private void handlePrivateSend(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        long to = longVal(msg.get("toUserId"));
        String body = bodyToStoredString(msg.get("body"));
        ImMessage m = chatService.sendPrivate(uid(ep), to, body);
        chatService.sendJson(ep, chatService.toPrivatePush(m));
        chatService.pushPrivateToPeer(m);
    }

    private void handleGroupSend(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        long gid = longVal(msg.get("groupId"));
        String body = bodyToStoredString(msg.get("body"));
        ImMessage m = chatService.sendGroup(uid(ep), gid, body);
        chatService.sendJson(ep, chatService.toGroupPush(m));
        chatService.broadcastGroupMessage(m);
    }

    private void handleGroupCreate(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        String name = msg.get("name") == null ? null : String.valueOf(msg.get("name"));
        List<Long> memberIds = readLongList(msg, "memberIds");
        Map<String, Object> res = chatService.createGroup(uid(ep), name, memberIds);
        chatService.sendJson(ep, res);
    }

    private List<Long> readLongList(Map<String, Object> msg, String key) {
        Object o = msg.get(key);
        if (o == null) {
            return List.of();
        }
        if (!(o instanceof List<?> list)) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (Object x : list) {
            if (x == null) {
                continue;
            }
            if (x instanceof Number n) {
                out.add(n.longValue());
            } else {
                out.add(Long.parseLong(String.valueOf(x)));
            }
        }
        return out;
    }

    private void handleGroupJoin(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        long gid = longVal(msg.get("groupId"));
        Map<String, Object> res = chatService.joinGroup(uid(ep), gid);
        chatService.sendJson(ep, res);
    }

    private void handleHistory(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        String mode = msg.get("mode") == null ? "" : String.valueOf(msg.get("mode"));
        Long beforeId = msg.get("beforeId") == null ? null : longVal(msg.get("beforeId"));
        Long afterId = msg.get("afterId") == null ? null : longVal(msg.get("afterId"));
        if (beforeId != null && afterId != null) {
            throw new IllegalArgumentException("beforeId 与 afterId 不能同时指定");
        }
        int limit = 50;
        if (msg.get("limit") instanceof Number n) {
            limit = n.intValue();
        }
        Map<String, Object> res;
        if ("P2P".equalsIgnoreCase(mode)) {
            long peer = longVal(msg.get("peerUserId"));
            res = chatService.historyP2P(uid(ep), peer, beforeId, afterId, limit);
        } else if ("GROUP".equalsIgnoreCase(mode)) {
            long gid = longVal(msg.get("groupId"));
            res = chatService.historyGroup(uid(ep), gid, beforeId, afterId, limit);
        } else {
            throw new IllegalArgumentException("HISTORY.mode 需为 P2P 或 GROUP");
        }
        chatService.sendJson(ep, res);
    }

    private void handleConversations(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        chatService.sendJson(ep, chatService.conversations(uid(ep)));
    }

    private void handleUserInfo(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        long target = longVal(msg.get("userId"));
        chatService.sendJson(ep, chatService.userInfo(target));
    }

    private void handleGroupInfo(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        long gid = longVal(msg.get("groupId"));
        chatService.sendJson(ep, chatService.groupInfo(gid, uid(ep)));
    }

    private void handleGroupRename(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        long gid = longVal(msg.get("groupId"));
        String newName = msg.get("name") == null ? "" : String.valueOf(msg.get("name"));
        Map<String, Object> res = chatService.renameGroup(uid(ep), gid, newName);
        chatService.sendJson(ep, res);
    }

    private void handleGroupSetAdmin(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        long gid = longVal(msg.get("groupId"));
        long target = longVal(msg.get("targetUserId"));
        Map<String, Object> res = chatService.setGroupAdmin(uid(ep), gid, target);
        chatService.sendJson(ep, res);
    }

    private void handleGroupRemoveAdmin(ImSessionEndpoint ep, Map<String, Object> msg) {
        requireAuth(ep);
        long gid = longVal(msg.get("groupId"));
        long target = longVal(msg.get("targetUserId"));
        Map<String, Object> res = chatService.removeGroupAdmin(uid(ep), gid, target);
        chatService.sendJson(ep, res);
    }

    private void err(ImSessionEndpoint ep, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "ERROR");
        m.put("message", message);
        chatService.sendJson(ep, m);
    }
}
