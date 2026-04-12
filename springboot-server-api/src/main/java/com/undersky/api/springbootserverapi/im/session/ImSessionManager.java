package com.undersky.api.springbootserverapi.im.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * userId -> 当前连接（Netty 或 Spring WebSocket 二选一在线；新连接挤掉旧连接）
 */
@Component
public class ImSessionManager {

    private final Map<Long, ImSessionEndpoint> userIdToEndpoint = new ConcurrentHashMap<>();

    public void bind(Long userId, ImSessionEndpoint endpoint) {
        endpoint.onBound(userId);
        ImSessionEndpoint old = userIdToEndpoint.put(userId, endpoint);
        if (old != null && old != endpoint && old.isActive()) {
            old.close();
        }
    }

    public void unbind(ImSessionEndpoint endpoint) {
        Long uid = endpoint.getBoundUserId();
        if (uid != null) {
            userIdToEndpoint.remove(uid, endpoint);
        }
    }

    public ImSessionEndpoint endpointOf(Long userId) {
        return userIdToEndpoint.get(userId);
    }

    /** 遍历当前在线会话（userId → endpoint） */
    public void forEachOnline(BiConsumer<Long, ImSessionEndpoint> consumer) {
        userIdToEndpoint.forEach(consumer);
    }

    /** 当前 IM 在线会话数（与在线用户数一致，多端登录同一账号只计 1） */
    public int onlineSessionCount() {
        return userIdToEndpoint.size();
    }
}
