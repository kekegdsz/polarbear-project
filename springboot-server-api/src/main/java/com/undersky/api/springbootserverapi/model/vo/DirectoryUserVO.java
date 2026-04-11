package com.undersky.api.springbootserverapi.model.vo;

/**
 * IM 通讯录：对外展示的用户摘要（不含设备 UUID、角色等敏感字段）
 */
public class DirectoryUserVO {

    private Long id;
    private String username;
    private String mobile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
