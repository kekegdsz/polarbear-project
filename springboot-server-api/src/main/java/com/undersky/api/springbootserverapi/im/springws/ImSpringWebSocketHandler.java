package com.undersky.api.springbootserverapi.im.springws;

import com.undersky.api.springbootserverapi.im.ImJsonMessageProcessor;
import com.undersky.api.springbootserverapi.im.session.ImSessionManager;
import com.undersky.api.springbootserverapi.im.session.ImSpringWsEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 与 Tomcat 同端口的 WebSocket（路径含 context-path 时为 /api/im/ws），便于经 Vite/Nginx 的 /api 反代。
 */
@Component
public class ImSpringWebSocketHandler extends TextWebSocketHandler {

    public static final String SESSION_EP = "imSpringWsEndpoint";

    private final ImJsonMessageProcessor messageProcessor;
    private final ImSessionManager sessionManager;

    public ImSpringWebSocketHandler(ImJsonMessageProcessor messageProcessor,
                                    ImSessionManager sessionManager) {
        this.messageProcessor = messageProcessor;
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        ImSpringWsEndpoint ep = new ImSpringWsEndpoint(session);
        session.getAttributes().put(SESSION_EP, ep);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        ImSpringWsEndpoint ep = (ImSpringWsEndpoint) session.getAttributes().get(SESSION_EP);
        if (ep == null) {
            return;
        }
        messageProcessor.handleText(ep, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ImSpringWsEndpoint ep = (ImSpringWsEndpoint) session.getAttributes().get(SESSION_EP);
        if (ep != null) {
            sessionManager.unbind(ep);
        }
    }
}
