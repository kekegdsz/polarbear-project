package com.undersky.api.springbootserverapi.model.vo;

/**
 * 管理后台用户统计
 */
public class UserStatsVO {

    private long totalUsers;
    private long todayUsers;
    private long vipUsers;

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
}

