package com.undersky.api.springbootserverapi.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 管理后台向群广播文本
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminImBroadcastRequest {

    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
