package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.LogRecordMapper;
import com.undersky.api.springbootserverapi.model.dto.PageResult;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.LogRecord;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 日志管理
 */
@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {

    private final LogRecordMapper logRecordMapper;

    public AdminLogController(LogRecordMapper logRecordMapper) {
        this.logRecordMapper = logRecordMapper;
    }

    /**
     * 1) 保存日志（落库：id、内容、创建时间、ack 是否已读）
     */
    @PostMapping
    public Result<LogRecord> create(@RequestBody CreateLogRequest req) {
        if (req == null || req.getAppId() == null || req.getAppId().isBlank()) {
            return Result.error("appId 不能为空");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            return Result.error("日志内容不能为空");
        }
        LogRecord record = new LogRecord();
        record.setAppId(req.getAppId().trim());
        record.setContent(req.getContent().trim());
        record.setAck(0);
        record.setCreatedAt(LocalDateTime.now());
        logRecordMapper.insert(record);
        return Result.success(record);
    }

    /**
     * 2) 通过 id 告诉后端已读
     */
    @PostMapping("/{id}/ack")
    public Result<Void> ack(@PathVariable Long id, @RequestBody(required = false) AckRequest req) {
        if (req == null || req.getAppId() == null || req.getAppId().isBlank()) {
            return Result.error("appId 不能为空");
        }
        LogRecord exists = logRecordMapper.selectById(id);
        if (exists == null) {
            return Result.error("日志不存在");
        }
        if (exists.getAppId() == null || !exists.getAppId().equals(req.getAppId().trim())) {
            return Result.error("appId 不匹配");
        }
        logRecordMapper.markAck(id);
        return Result.success();
    }

    /**
     * 3) 获取未读日志接口，返回 list（倒序）
     * 兼容扩展：body 传 unreadOnly=false 时返回全部
     */
    @PostMapping("/unread")
    public Result<PageResult<LogRecord>> unreadList(@RequestBody(required = false) UnreadListRequest req) {
        if (req == null || req.getAppId() == null || req.getAppId().isBlank()) {
            return Result.error("appId 不能为空");
        }
        boolean unreadOnly = req.getUnreadOnly() == null || req.getUnreadOnly();
        Integer ack = unreadOnly ? 0 : null;
        int page = req.getPage() == null || req.getPage() < 1 ? 1 : req.getPage();
        int size = req.getSize() == null || req.getSize() < 1 || req.getSize() > 100 ? 10 : req.getSize();
        int offset = (page - 1) * size;
        String appId = req.getAppId().trim();
        long total = logRecordMapper.countByFilter(appId, ack);
        List<LogRecord> list = logRecordMapper.selectList(appId, ack, offset, size);
        return Result.success(new PageResult<>(total, list));
    }

    public static class UnreadListRequest {
        private String appId;
        private Boolean unreadOnly;
        private Integer page;
        private Integer size;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public Boolean getUnreadOnly() {
            return unreadOnly;
        }

        public void setUnreadOnly(Boolean unreadOnly) {
            this.unreadOnly = unreadOnly;
        }

        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }
    }

    public static class CreateLogRequest {
        private String appId;
        private String content;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class AckRequest {
        private String appId;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }
    }
}

