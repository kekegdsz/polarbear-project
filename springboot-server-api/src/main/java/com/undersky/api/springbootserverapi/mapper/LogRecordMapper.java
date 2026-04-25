package com.undersky.api.springbootserverapi.mapper;

import com.undersky.api.springbootserverapi.model.entity.LogRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogRecordMapper {

    int insert(LogRecord record);

    int markAck(@Param("id") Long id);

    LogRecord selectById(@Param("id") Long id);

    List<LogRecord> selectList(@Param("appId") String appId,
                               @Param("ack") Integer ack,
                               @Param("offset") int offset,
                               @Param("size") int size);

    long countByFilter(@Param("appId") String appId, @Param("ack") Integer ack);
}

