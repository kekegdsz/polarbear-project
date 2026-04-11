package com.undersky.api.springbootserverapi.im.session;

/**
 * IM 连接抽象：Netty Channel 与 Spring WebSocket 共用会话表。
 */
public interface ImSessionEndpoint {

    void onBound(long userId);

    Long getBoundUserId();

    boolean isActive();

    void sendJsonString(String json);

    void close();
}
