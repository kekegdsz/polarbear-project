package com.undersky.api.springbootserverapi.im.session;

import io.netty.util.AttributeKey;

public final class ImChannelAttributes {

    public static final AttributeKey<Long> USER_ID = AttributeKey.valueOf("imUserId");
    public static final AttributeKey<ImNettyEndpoint> ENDPOINT = AttributeKey.valueOf("imNettyEndpoint");

    private ImChannelAttributes() {
    }
}
