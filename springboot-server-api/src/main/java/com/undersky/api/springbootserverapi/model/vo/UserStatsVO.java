package com.undersky.api.springbootserverapi.model.vo;

/**
 * 管理后台用户统计
 */
public class UserStatsVO {

    private long totalUsers;
    private long todayUsers;
    private long vipUsers;
    /** 当前 IM WebSocket 在线用户数 */
    private long onlineImUsers;

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTodayUsers() {
        return todayUsers;
    }

    public void setTodayUsers(long todayUsers) {
        this.todayUsers = todayUsers;
    }

    public long getVipUsers() {
        return vipUsers;
    }

    public void setVipUsers(long vipUsers) {
        this.vipUsers = vipUsers;
    }

    public long getOnlineImUsers() {
        return onlineImUsers;
    }

    public void setOnlineImUsers(long onlineImUsers) {
        this.onlineImUsers = onlineImUsers;
    }
}

