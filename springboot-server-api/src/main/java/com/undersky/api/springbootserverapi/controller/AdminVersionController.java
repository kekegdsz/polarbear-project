package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.AppVersionMapper;
import com.undersky.api.springbootserverapi.model.dto.PageResult;
import com.undersky.api.springbootserverapi.model.dto.PublishVersionRequest;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.AppVersion;
import com.undersky.api.springbootserverapi.model.vo.AppVersionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台 - 版本管理接口
 * 渠道：android / ios / ohos
 */
@RestController
@RequestMapping("/admin/versions")
public class AdminVersionController {

    private final AppVersionMapper appVersionMapper;

    public AdminVersionController(AppVersionMapper appVersionMapper) {
        this.appVersionMapper = appVersionMapper;
    }

    /**
     * 版本列表（分页 + 筛选）
     */
    @GetMapping
    public Result<PageResult<AppVersionVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String keyword) {
        if (page < 1) page = 1;
        if (size <= 0 || size > 100) size = 10;
        int offset = (page - 1) * size;

        long total = appVersionMapper.countByFilter(channel, keyword);
        List<AppVersion> list = appVersionMapper.selectList(offset, size, channel, keyword);
        List<AppVersionVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());

        return Result.success(new PageResult<>(total, voList));
    }

    /**
     * 发布版本
     */
    @PostMapping
    public Result<AppVersionVO> publish(@Valid @RequestBody PublishVersionRequest req) {
        String ch = req.getChannel().toLowerCase();
        if (!"android".equals(ch) && !"ios".equals(ch) && !"ohos".equals(ch)) {
            return Result.error("渠道必须是 android、ios 或 ohos");
        }

        AppVersion version = new AppVersion();
        version.setVersionName(req.getVersionName());
        version.setVersionCode(req.getVersionCode());
        version.setChannel(ch);
        version.setDownloadUrl(req.getDownloadUrl());
        version.setReleaseNotes(req.getReleaseNotes());
        version.setPublished(true);
        LocalDateTime now = LocalDateTime.now();
        version.setCreatedAt(now);
        version.setUpdatedAt(now);

        appVersionMapper.insert(version);
        return Result.success(toVO(version));
    }

    /**
     * 删除版本
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AppVersion v = appVersionMapper.selectById(id);
        if (v == null) {
            return Result.error("版本不存在");
        }
        appVersionMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 停止版本（App 查不到此版本）
     */
    @PatchMapping("/{id}/stop")
    public Result<AppVersionVO> stop(@PathVariable Long id) {
        AppVersion v = appVersionMapper.selectById(id);
        if (v == null) {
            return Result.error("版本不存在");
        }
        v.setPublished(false);
        v.setUpdatedAt(LocalDateTime.now());
        appVersionMapper.updateById(v);
        return Result.success(toVO(v));
    }

    /**
     * 重新发布版本
     */
    @PatchMapping("/{id}/publish")
    public Result<AppVersionVO> republish(@PathVariable Long id) {
        AppVersion v = appVersionMapper.selectById(id);
        if (v == null) {
            return Result.error("版本不存在");
        }
        v.setPublished(true);
        v.setUpdatedAt(LocalDateTime.now());
        appVersionMapper.updateById(v);
        return Result.success(toVO(v));
    }

    private AppVersionVO toVO(AppVersion v) {
        AppVersionVO vo = new AppVersionVO();
        vo.setId(v.getId());
        vo.setVersionName(v.getVersionName());
        vo.setVersionCode(v.getVersionCode());
        vo.setChannel(v.getChannel());
        vo.setDownloadUrl(v.getDownloadUrl());
        vo.setReleaseNotes(v.getReleaseNotes());
        vo.setPublished(v.getPublished());
        vo.setCreatedAt(v.getCreatedAt());
        return vo;
    }
}
