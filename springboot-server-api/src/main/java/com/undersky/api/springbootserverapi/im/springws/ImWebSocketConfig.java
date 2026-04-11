package com.undersky.api.springbootserverapi.im.springws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ImWebSocketConfig implements WebSocketConfigurer {

    private final ImSpringWebSocketHandler imSpringWebSocketHandler;

    public ImWebSocketConfig(ImSpringWebSocketHandler imSpringWebSocketHandler) {
        this.imSpringWebSocketHandler = imSpringWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(imSpringWebSocketHandler, "/im/ws")
                .setAllowedOriginPatterns("*");
    }
}
