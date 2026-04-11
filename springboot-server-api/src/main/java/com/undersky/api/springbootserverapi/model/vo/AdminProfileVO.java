package com.undersky.api.springbootserverapi.model.vo;

/**
 * 管理员个人信息（用于左上角 "hello xxxx" 展示）
 */
public class AdminProfileVO {

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
