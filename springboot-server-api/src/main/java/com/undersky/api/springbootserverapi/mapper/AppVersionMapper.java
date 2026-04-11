package com.undersky.api.springbootserverapi.mapper;

import com.undersky.api.springbootserverapi.model.entity.AppVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 版本 Mapper
 */
@Mapper
public interface AppVersionMapper {

    List<AppVersion> selectList(@Param("offset") int offset,
                                @Param("size") int size,
                                @Param("channel") String channel,
                                @Param("keyword") String keyword);

    long countByFilter(@Param("channel") String channel,
                      @Param("keyword") String keyword);

    AppVersion selectById(@Param("id") Long id);

    AppVersion selectLatestPublished(@Param("channel") String channel);

    int insert(AppVersion version);

    int updateById(AppVersion version);

    int deleteById(@Param("id") Long id);
}
