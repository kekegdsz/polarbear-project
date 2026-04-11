package com.undersky.api.springbootserverapi.im.session;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public final class ImSpringWsEndpoint implements ImSessionEndpoint {

    private final WebSocketSession session;
    private volatile Long boundUserId;

    public ImSpringWsEndpoint(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public void onBound(long userId) {
        this.boundUserId = userId;
    }

    @Override
    public Long getBoundUserId() {
        return boundUserId;
    }

    @Override
    public boolean isActive() {
        return session != null && session.isOpen();
    }

    @Override
    public void sendJsonString(String json) {
        if (!isActive()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void close() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public WebSocketSession session() {
        return session;
    }
}
