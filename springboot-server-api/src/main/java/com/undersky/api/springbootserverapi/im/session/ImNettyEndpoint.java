package com.undersky.api.springbootserverapi.im.session;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public final class ImNettyEndpoint implements ImSessionEndpoint {

    private final Channel channel;
    private volatile Long boundUserId;

    public ImNettyEndpoint(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void onBound(long userId) {
        this.boundUserId = userId;
        channel.attr(ImChannelAttributes.USER_ID).set(userId);
    }

    @Override
    public Long getBoundUserId() {
        return boundUserId;
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    @Override
    public void sendJsonString(String json) {
        if (isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }

    @Override
    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }

    public Channel channel() {
        return channel;
    }
}
