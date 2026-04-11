package com.undersky.api.springbootserverapi.im.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "im.netty")
public class ImNettyProperties {

    /**
     * WebSocket 监听端口（与 Tomcat 分离）
     */
    private int port = 19080;

    /**
     * WebSocket 路径，如 /im/ws
     */
    private String path = "/im/ws";

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
