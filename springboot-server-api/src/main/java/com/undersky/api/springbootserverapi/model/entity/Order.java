package com.undersky.api.springbootserverapi.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 * status: pending-待付款, ordered-已下单, paid-已付款
 * payment_type: wechat-微信, alipay-支付宝
 */
public class Order {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ORDERED = "ordered";
    public static final String STATUS_PAID = "paid";
    public static final String PAYMENT_WECHAT = "wechat";
    public static final String PAYMENT_ALIPAY = "alipay";

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String paymentType;
    private String paymentTradeNo;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getPaymentTradeNo() { return paymentTradeNo; }
    public void setPaymentTradeNo(String paymentTradeNo) { this.paymentTradeNo = paymentTradeNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
