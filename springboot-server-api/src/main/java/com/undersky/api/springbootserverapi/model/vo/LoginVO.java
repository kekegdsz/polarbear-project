package com.undersky.api.springbootserverapi.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 登录/注册响应（参考 vlserver DeviceTransformer 结构）
 */
public class LoginVO {

    private String userId;
    private String mobile;
    @JsonProperty("is_vip")
    private boolean isVip;
    private String vip;
    private String vipFormat;
    private Long vipExpireTime;
    private String token;
    private String role;
    private Long registerTime;
    private Long responseTime;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public boolean isVip() {
        return isVip;
    }

    public void setVipFlag(boolean vip) {
        isVip = vip;
    }

    public String getVip() {
        return vip;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public String getVipFormat() {
        return vipFormat;
    }

    public void setVipFormat(String vipFormat) {
        this.vipFormat = vipFormat;
    }

    public Long getVipExpireTime() {
        return vipExpireTime;
    }

    public void setVipExpireTime(Long vipExpireTime) {
        this.vipExpireTime = vipExpireTime;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Long registerTime) {
        this.registerTime = registerTime;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }
}
