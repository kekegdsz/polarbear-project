package com.undersky.api.springbootserverapi.mapper;

import com.undersky.api.springbootserverapi.model.entity.LogRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LogRecordMapper {

    int insert(LogRecord record);

    int insertBatch(@Param("list") List<LogRecord> list);

    int markAck(@Param("id") Long id);

    LogRecord selectById(@Param("id") Long id);

    List<LogRecord> selectList(@Param("appId") String appId,
                               @Param("employeeNo") String employeeNo,
                               @Param("durationGt") Long durationGt,
                               @Param("createdStart") LocalDateTime createdStart,
                               @Param("createdEnd") LocalDateTime createdEnd,
                               @Param("ack") Integer ack,
                               @Param("offset") int offset,
                               @Param("size") int size);

    long countByFilter(@Param("appId") String appId,
                       @Param("employeeNo") String employeeNo,
                       @Param("durationGt") Long durationGt,
                       @Param("createdStart") LocalDateTime createdStart,
                       @Param("createdEnd") LocalDateTime createdEnd,
                       @Param("ack") Integer ack);

    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}

