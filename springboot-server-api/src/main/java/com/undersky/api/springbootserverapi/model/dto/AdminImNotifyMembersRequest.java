package com.undersky.api.springbootserverapi.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 向群内指定用户发系统私聊（每人一条 P2P）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminImNotifyMembersRequest {

    private List<Long> userIds;
    private String body;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
