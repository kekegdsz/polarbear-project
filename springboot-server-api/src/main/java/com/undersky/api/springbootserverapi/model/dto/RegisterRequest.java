package com.undersky.api.springbootserverapi.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

/**
 * 注册请求
 * - 仅 deviceUuid：设备自动注册（账号、密码、手机号可选）
 * - username + password：账号注册（可带 deviceUuid 绑定设备）
 * - 至少需 deviceUuid 或 (username+password)
 */
public class RegisterRequest {

    @Size(max = 64, message = "设备 UUID 过长")
    private String deviceUuid;

    @Size(max = 50, message = "用户名过长")
    private String username;

    @Size(max = 32, message = "密码过长")
    private String password;

    @Size(max = 20, message = "手机号格式错误")
    private String mobile;

    @AssertTrue(message = "请提供设备 UUID 或 用户名+密码")
    public boolean isValidRequest() {
        boolean hasDeviceUuid = StringUtils.hasText(deviceUuid);
        boolean hasAccount = StringUtils.hasText(username) && StringUtils.hasText(password);
        return hasDeviceUuid || hasAccount;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
