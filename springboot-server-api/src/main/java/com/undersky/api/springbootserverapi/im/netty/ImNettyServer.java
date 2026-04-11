package com.undersky.api.springbootserverapi.im.netty;

import com.undersky.api.springbootserverapi.im.ImJsonMessageProcessor;
import com.undersky.api.springbootserverapi.im.config.ImNettyProperties;
import com.undersky.api.springbootserverapi.im.service.ImChatService;
import com.undersky.api.springbootserverapi.im.session.ImSessionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 基于 Netty 的 IM WebSocket 服务，随 Spring 启动。
 */
@Component
public class ImNettyServer {

    private static final Logger log = LoggerFactory.getLogger(ImNettyServer.class);

    private final ImNettyProperties properties;
    private final ImJsonMessageProcessor messageProcessor;
    private final ImSessionManager sessionManager;
    private final ImChatService chatService;
    private final Environment environment;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public ImNettyServer(ImNettyProperties properties,
                         ImJsonMessageProcessor messageProcessor,
                         ImSessionManager sessionManager,
                         ImChatService chatService,
                         Environment environment) {
        this.properties = properties;
        this.messageProcessor = messageProcessor;
        this.sessionManager = sessionManager;
        this.chatService = chatService;
        this.environment = environment;
    }

    @Order(100)
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (bossGroup != null) {
            return;
        }
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ImChannelInitializer(
                            messageProcessor,
                            sessionManager,
                            chatService,
                            properties.getPath()));
            ChannelFuture bind = b.bind(properties.getPort()).sync();
            serverChannel = bind.channel();
            log.info("IM Netty WebSocket 已启动: ws://0.0.0.0:{}{}", properties.getPort(), properties.getPath());
            String port = environment.getProperty("server.port", "8082");
            String ctx = environment.getProperty("server.servlet.context-path", "");
            if ("/".equals(ctx)) {
                ctx = "";
            }
            log.info("IM 测试页(静态): http://127.0.0.1:{}{}/im/im-chat", port, ctx);
            log.info("IM WebSocket(推荐/Vite反代): ws://127.0.0.1:{}{}/im/ws", port, ctx);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("IM Netty 启动被中断", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("IM Netty 已关闭");
    }
}
