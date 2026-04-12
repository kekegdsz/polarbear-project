package com.undersky.api.springbootserverapi.im.mapper;

import com.undersky.api.springbootserverapi.im.model.ImGroup;
import com.undersky.api.springbootserverapi.model.vo.ImGroupAdminListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImGroupMapper {

    int insert(ImGroup group);

    ImGroup findById(@Param("id") Long id);

    List<Long> listMemberUserIds(@Param("groupId") Long groupId);

    int updateName(@Param("groupId") Long groupId, @Param("name") String name);

    List<ImGroupAdminListVO> listAllForAdmin();
}
