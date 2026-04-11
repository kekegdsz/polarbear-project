package com.undersky.api.springbootserverapi.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改当前用户展示信息（昵称等）
 */
public class UpdateProfileRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最多32个字符")
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
