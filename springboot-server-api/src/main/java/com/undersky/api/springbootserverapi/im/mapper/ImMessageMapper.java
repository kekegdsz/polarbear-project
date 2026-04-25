package com.undersky.api.springbootserverapi.im.mapper;

import com.undersky.api.springbootserverapi.im.model.ImMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImMessageMapper {

    int insert(ImMessage m);

    List<ImMessage> listP2PHistory(
            @Param("userId") Long userId,
            @Param("peerUserId") Long peerUserId,
            @Param("beforeId") Long beforeId,
            @Param("limit") int limit);

    List<ImMessage> listGroupHistory(
            @Param("groupId") Long groupId,
            @Param("beforeId") Long beforeId,
            @Param("limit") int limit);

    List<ImMessage> listP2PHistoryAfter(
            @Param("userId") Long userId,
            @Param("peerUserId") Long peerUserId,
            @Param("afterId") Long afterId,
            @Param("limit") int limit);

    List<ImMessage> listGroupHistoryAfter(
            @Param("groupId") Long groupId,
            @Param("afterId") Long afterId,
            @Param("limit") int limit);

    List<ImMessage> listRecentP2PPeers(@Param("userId") Long userId);

    List<ImMessage> listRecentGroupSummaries(@Param("userId") Long userId);

    int deleteByGroupId(@Param("groupId") Long groupId);
}
