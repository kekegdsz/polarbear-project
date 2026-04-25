package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.AppMapper;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.App;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 管理后台 - 应用管理（用于生成 appId，持久化保存）
 */
@RestController
@RequestMapping("/admin/apps")
public class AdminAppController {

    private final AppMapper appMapper;

    public AdminAppController(AppMapper appMapper) {
        this.appMapper = appMapper;
    }

    /**
     * 创建应用，生成 appId
     */
    @PostMapping
    public Result<App> create(@RequestBody CreateAppRequest req) {
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            return Result.error("应用名称不能为空");
        }
        App app = new App();
        app.setAppId(UUID.randomUUID().toString().replace("-", ""));
        app.setName(req.getName().trim());
        app.setRemark(req.getRemark() == null ? null : req.getRemark().trim());
        app.setCreatedAt(LocalDateTime.now());
        appMapper.insert(app);
        return Result.success(app);
    }

    /**
     * 应用列表
     */
    @PostMapping("/list")
    public Result<List<App>> list() {
        return Result.success(appMapper.selectAll());
    }

    public static class CreateAppRequest {
        private String name;
        private String remark;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}

