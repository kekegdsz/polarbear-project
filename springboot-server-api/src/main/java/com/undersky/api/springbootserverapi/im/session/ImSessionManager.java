package com.undersky.api.springbootserverapi.im.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
}
