package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.AppVersionMapper;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.AppVersion;
import com.undersky.api.springbootserverapi.model.vo.AppVersionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开接口 - 客户端获取最新版本（无需鉴权）
 */
@RestController
@RequestMapping("/versions")
public class VersionController {

    private final AppVersionMapper appVersionMapper;

    public VersionController(AppVersionMapper appVersionMapper) {
        this.appVersionMapper = appVersionMapper;
    }

    /**
     * 获取指定渠道最新已发布版本
     * GET /api/versions/latest?channel=android
     */
    @GetMapping("/latest")
    public Result<AppVersionVO> latest(@RequestParam(defaultValue = "android") String channel) {
        String ch = channel.toLowerCase();
        if (!"android".equals(ch) && !"ios".equals(ch) && !"ohos".equals(ch)) {
            return Result.error("渠道必须是 android、ios 或 ohos");
        }
        AppVersion v = appVersionMapper.selectLatestPublished(ch);
        if (v == null) {
            return Result.success(null);
        }
        AppVersionVO vo = new AppVersionVO();
        vo.setId(v.getId());
        vo.setVersionName(v.getVersionName());
        vo.setVersionCode(v.getVersionCode());
        vo.setChannel(v.getChannel());
        vo.setDownloadUrl(v.getDownloadUrl());
        vo.setReleaseNotes(v.getReleaseNotes());
        vo.setPublished(v.getPublished());
        vo.setCreatedAt(v.getCreatedAt());
        return Result.success(vo);
    }
}
