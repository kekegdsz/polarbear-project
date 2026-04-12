package com.undersky.api.springbootserverapi.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public class ImGroupAdminDetailVO {

    private Long id;
    private String name;
    private Long ownerUserId;
    private LocalDateTime createdAt;
    private int memberCount;
    private List<ImGroupMemberAdminVO> members;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public List<ImGroupMemberAdminVO> getMembers() {
        return members;
    }

    public void setMembers(List<ImGroupMemberAdminVO> members) {
        this.members = members;
    }
}
