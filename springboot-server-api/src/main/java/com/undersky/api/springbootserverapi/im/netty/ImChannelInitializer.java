package com.undersky.api.springbootserverapi.im.netty;

import com.undersky.api.springbootserverapi.im.ImJsonMessageProcessor;
import com.undersky.api.springbootserverapi.im.session.ImSessionManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ImChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ImJsonMessageProcessor messageProcessor;
    private final ImSessionManager sessionManager;
    private final String websocketPath;

    public ImChannelInitializer(ImJsonMessageProcessor messageProcessor,
                                ImSessionManager sessionManager,
                                String websocketPath) {
        this.messageProcessor = messageProcessor;
        this.sessionManager = sessionManager;
        this.websocketPath = websocketPath;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(65536))
                .addLast(new ChunkedWriteHandler())
                .addLast(new WebSocketServerProtocolHandler(websocketPath))
                .addLast(new ImWebSocketHandler(messageProcessor, sessionManager));
    }
}
