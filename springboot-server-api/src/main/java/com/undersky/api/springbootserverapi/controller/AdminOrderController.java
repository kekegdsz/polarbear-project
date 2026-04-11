package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.OrderMapper;
import com.undersky.api.springbootserverapi.mapper.UserMapper;
import com.undersky.api.springbootserverapi.model.dto.PageResult;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.Order;
import com.undersky.api.springbootserverapi.model.entity.User;
import com.undersky.api.springbootserverapi.model.vo.OrderStatsVO;
import com.undersky.api.springbootserverapi.model.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理后台 - 订单管理
 * 支持微信/支付宝，状态：待付款/已下单/已付款
 */
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    public AdminOrderController(OrderMapper orderMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
    }

    /**
     * 订单统计
     */
    @GetMapping("/stats")
    public Result<OrderStatsVO> stats() {
        OrderStatsVO stats = orderMapper.selectStats();
        if (stats == null) {
            stats = new OrderStatsVO();
            stats.setTotalCount(0);
            stats.setPendingCount(0);
            stats.setOrderedCount(0);
            stats.setPaidCount(0);
            stats.setTotalAmount(BigDecimal.ZERO);
            stats.setPaidAmount(BigDecimal.ZERO);
        }
        return Result.success(stats);
    }

    /**
     * 订单列表（分页 + 筛选）
     */
    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentType,
            @RequestParam(required = false) String keyword) {
        if (page < 1) page = 1;
        if (size <= 0 || size > 100) size = 10;
        int offset = (page - 1) * size;

        long total = orderMapper.countByFilter(status, paymentType, keyword);
        List<Order> list = orderMapper.selectList(offset, size, status, paymentType, keyword);
        List<OrderVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());

        return Result.success(new PageResult<>(total, voList));
    }

    /**
     * 创建订单（测试用）
     */
    @PostMapping
    public Result<OrderVO> create(@RequestBody CreateOrderRequest req) {
        String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(req.getUserId());
        order.setAmount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO);
        order.setStatus(req.getStatus() != null ? req.getStatus() : Order.STATUS_PENDING);
        order.setPaymentType(req.getPaymentType());
        order.setRemark(req.getRemark());
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        orderMapper.insert(order);
        return Result.success(toVO(order));
    }

    /**
     * 更新订单状态
     */
    @PatchMapping("/{id}/status")
    public Result<OrderVO> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest req) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (req.getStatus() != null) {
            order.setStatus(req.getStatus());
        }
        if (req.getPaymentTradeNo() != null) {
            order.setPaymentTradeNo(req.getPaymentTradeNo());
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.success(toVO(order));
    }

    private OrderVO toVO(Order o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setUserId(o.getUserId());
        vo.setAmount(o.getAmount());
        vo.setStatus(o.getStatus());
        vo.setStatusLabel(statusLabel(o.getStatus()));
        vo.setPaymentType(o.getPaymentType());
        vo.setPaymentTypeLabel(paymentTypeLabel(o.getPaymentType()));
        vo.setPaymentTradeNo(o.getPaymentTradeNo());
        vo.setRemark(o.getRemark());
        vo.setCreatedAt(o.getCreatedAt());
        vo.setUpdatedAt(o.getUpdatedAt());
        if (o.getUserId() != null) {
            User u = userMapper.selectById(o.getUserId());
            if (u != null) vo.setDeviceUuid(u.getDeviceUuid());
        }
        return vo;
    }

    private String statusLabel(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "pending" -> "待付款";
            case "ordered" -> "已下单";
            case "paid" -> "已付款";
            default -> s;
        };
    }

    private String paymentTypeLabel(String s) {
        if (s == null) return "-";
        return switch (s) {
            case "wechat" -> "微信";
            case "alipay" -> "支付宝";
            default -> s;
        };
    }

    public static class CreateOrderRequest {
        private Long userId;
        private BigDecimal amount;
        private String status;
        private String paymentType;
        private String remark;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPaymentType() { return paymentType; }
        public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class UpdateStatusRequest {
        private String status;
        private String paymentTradeNo;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPaymentTradeNo() { return paymentTradeNo; }
        public void setPaymentTradeNo(String paymentTradeNo) { this.paymentTradeNo = paymentTradeNo; }
    }
}
