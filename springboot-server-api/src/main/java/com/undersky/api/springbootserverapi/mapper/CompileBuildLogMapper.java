package com.undersky.api.springbootserverapi.mapper;

import com.undersky.api.springbootserverapi.model.entity.CompileBuildLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CompileBuildLogMapper {

    int insert(CompileBuildLog row);

    List<CompileBuildLog> selectList(@Param("appId") String appId,
                                     @Param("createdStart") LocalDateTime createdStart,
                                     @Param("createdEnd") LocalDateTime createdEnd,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    long countByFilter(@Param("appId") String appId,
                       @Param("createdStart") LocalDateTime createdStart,
                       @Param("createdEnd") LocalDateTime createdEnd);
}
