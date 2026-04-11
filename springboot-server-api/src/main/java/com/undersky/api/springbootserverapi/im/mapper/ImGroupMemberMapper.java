package com.undersky.api.springbootserverapi.im.mapper;

import com.undersky.api.springbootserverapi.im.model.ImGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImGroupMemberMapper {

    int insert(ImGroupMember m);

    int deleteMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    int countMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
