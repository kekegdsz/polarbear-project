package com.undersky.api.springbootserverapi.mapper;

import com.undersky.api.springbootserverapi.model.entity.Order;
import com.undersky.api.springbootserverapi.model.vo.OrderStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    List<Order> selectList(@Param("offset") int offset, @Param("size") int size,
                           @Param("status") String status, @Param("paymentType") String paymentType,
                           @Param("keyword") String keyword);

    long countByFilter(@Param("status") String status, @Param("paymentType") String paymentType,
                       @Param("keyword") String keyword);

    OrderStatsVO selectStats();

    Order selectById(Long id);

    Order selectByOrderNo(String orderNo);

    int insert(Order order);

    int updateById(Order order);
}
