package com.undersky.api.springbootserverapi.im.model;

import java.time.LocalDateTime;

public class ImGroupMember {

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";

    private Long groupId;
    private Long userId;
    /** {@link #ROLE_OWNER} / {@link #ROLE_ADMIN} / {@link #ROLE_MEMBER} */
    private String role;
    private LocalDateTime joinedAt;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
