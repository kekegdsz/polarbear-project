package com.undersky.api.springbootserverapi.model.vo;

import java.math.BigDecimal;

/**
 * 订单统计 VO
 */
public class OrderStatsVO {

    private long totalCount;
    private long pendingCount;
    private long orderedCount;
    private long paidCount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    public long getOrderedCount() { return orderedCount; }
    public void setOrderedCount(long orderedCount) { this.orderedCount = orderedCount; }
    public long getPaidCount() { return paidCount; }
    public void setPaidCount(long paidCount) { this.paidCount = paidCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
}
