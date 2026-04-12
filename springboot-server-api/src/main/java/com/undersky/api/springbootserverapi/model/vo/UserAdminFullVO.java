package com.undersky.api.springbootserverapi.model.vo;

import java.time.LocalDateTime;

/**
 * 管理后台用户完整字段（不含密码）；token 脱敏。
 */
public class UserAdminFullVO {

    private Long id;
    private String deviceUuid;
    private String username;
    private String nickname;
    private String mobile;
    /** 是否有有效 token（不返回原文） */
    private boolean hasToken;
    private String tokenMasked;
    private LocalDateTime tokenExpireTime;
    private String vip;
    private LocalDateTime vipExpireTime;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 当前 IM WebSocket 是否在线 */
    private boolean online;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public boolean isHasToken() {
        return hasToken;
    }

    public void setHasToken(boolean hasToken) {
        this.hasToken = hasToken;
    }

    public String getTokenMasked() {
        return tokenMasked;
    }

    public void setTokenMasked(String tokenMasked) {
        this.tokenMasked = tokenMasked;
    }

    public LocalDateTime getTokenExpireTime() {
        return tokenExpireTime;
    }

    public void setTokenExpireTime(LocalDateTime tokenExpireTime) {
        this.tokenExpireTime = tokenExpireTime;
    }

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public LocalDateTime getVipExpireTime() {
        return vipExpireTime;
    }

    public void setVipExpireTime(LocalDateTime vipExpireTime) {
        this.vipExpireTime = vipExpireTime;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
