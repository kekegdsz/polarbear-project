package com.undersky.api.springbootserverapi.mapper;

import com.undersky.api.springbootserverapi.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper {

    User selectById(Long id);

    User selectByUsername(@Param("username") String username);

    User selectByMobile(@Param("mobile") String mobile);

    User selectByDeviceUuid(@Param("deviceUuid") String deviceUuid);

    User selectByToken(@Param("token") String token);

    long countAll();

    long countToday();

    long countVip();

    List<User> selectUsers(@Param("offset") int offset,
                           @Param("size") int size,
                           @Param("keyword") String keyword);

    int countByUsername(@Param("username") String username);

    int countByMobile(@Param("mobile") String mobile);

    int insert(User user);

    int updateById(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("updatedAt") java.time.LocalDateTime updatedAt);

    int backfillBlankNicknames();
}
