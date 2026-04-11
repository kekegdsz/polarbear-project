package com.undersky.api.springbootserverapi.im.netty;

import com.undersky.api.springbootserverapi.im.ImJsonMessageProcessor;
import com.undersky.api.springbootserverapi.im.service.ImChatService;
import com.undersky.api.springbootserverapi.im.session.ImChannelAttributes;
import com.undersky.api.springbootserverapi.im.session.ImNettyEndpoint;
import com.undersky.api.springbootserverapi.im.session.ImSessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * Netty WebSocket 文本帧入口，业务逻辑见 {@link ImJsonMessageProcessor}。
 */
public class ImWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final ImJsonMessageProcessor messageProcessor;
    private final ImSessionManager sessionManager;
    private final ImChatService chatService;

    public ImWebSocketHandler(ImJsonMessageProcessor messageProcessor,
                            ImSessionManager sessionManager,
                            ImChatService chatService) {
        this.messageProcessor = messageProcessor;
        this.sessionManager = sessionManager;
        this.chatService = chatService;
    }

    private static ImNettyEndpoint endpoint(Channel ch) {
        ImNettyEndpoint e = ch.attr(ImChannelAttributes.ENDPOINT).get();
        if (e == null) {
            e = new ImNettyEndpoint(ch);
            ch.attr(ImChannelAttributes.ENDPOINT).set(e);
        }
        return e;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ImNettyEndpoint ep = ctx.channel().attr(ImChannelAttributes.ENDPOINT).get();
        if (ep != null) {
            Long uid = ep.getBoundUserId();
            sessionManager.unbind(ep);
            if (uid != null) {
                chatService.broadcastPresence(uid, false);
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        messageProcessor.handleText(endpoint(ctx.channel()), frame.text());
    }
}
