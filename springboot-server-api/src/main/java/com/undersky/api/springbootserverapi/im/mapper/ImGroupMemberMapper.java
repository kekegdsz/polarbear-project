package com.undersky.api.springbootserverapi.im.mapper;

import com.undersky.api.springbootserverapi.im.model.ImGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImGroupMemberMapper {

    int insert(ImGroupMember m);

    int deleteMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    int countMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    String selectRole(@Param("groupId") Long groupId, @Param("userId") Long userId);

    int updateRole(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("role") String role);

    List<ImGroupMember> listByGroupId(@Param("groupId") Long groupId);
}
