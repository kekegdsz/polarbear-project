package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.LogRecordMapper;
import com.undersky.api.springbootserverapi.model.dto.PageResult;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.LogRecord;
import com.undersky.api.springbootserverapi.service.LogIngestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理后台 - 日志管理
 */
@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {
    private static final Pattern HTTP_LATENCY_PATTERN = Pattern.compile("\"httpLatency\"\\s*:\\s*(\\d+)");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LogRecordMapper logRecordMapper;
    private final LogIngestService logIngestService;

    public AdminLogController(LogRecordMapper logRecordMapper, LogIngestService logIngestService) {
        this.logRecordMapper = logRecordMapper;
        this.logIngestService = logIngestService;
    }

    /**
     * 1) 保存日志（落库：id、内容、创建时间、ack 是否已读）
     */
    @PostMapping
    public Result<LogRecord> create(@RequestBody CreateLogRequest req) {
        if (req == null || req.getAppId() == null || req.getAppId().isBlank()) {
            return Result.error("appId 不能为空");
        }
        String rawContent = req.getRaw() != null && !req.getRaw().isBlank() ? req.getRaw() : req.getContent();
        if (rawContent == null || rawContent.isBlank()) {
            return Result.error("日志内容不能为空");
        }
        LogRecord record = new LogRecord();
        record.setAppId(req.getAppId().trim());
        if (req.getEmployeeNo() != null && !req.getEmployeeNo().isBlank()) {
            record.setEmployeeNo(req.getEmployeeNo().trim());
        }
        record.setContent(rawContent.trim());
        record.setDurationMs(extractDurationMs(rawContent));
        record.setAck(0);
        record.setCreatedAt(LocalDateTime.now());
        boolean accepted = logIngestService.ingest(record);
        if (!accepted) {
            return Result.error("日志写入繁忙，请稍后重试");
        }
        return Result.success(record);
    }

    /**
     * 批量保存日志（同一 appId）
     */
    @PostMapping("/batch")
    public Result<BatchCreateLogResponse> createBatch(@RequestBody BatchCreateLogRequest req) {
        if (req == null || req.getAppId() == null || req.getAppId().isBlank()) {
            return Result.error("appId 不能为空");
        }
        List<String> sourceContents = req.getRaws() != null && !req.getRaws().isEmpty() ? req.getRaws() : req.getContents();
        if (sourceContents == null || sourceContents.isEmpty()) {
            return Result.error("日志内容列表不能为空");
        }
        if (sourceContents.size() > 1000) {
            return Result.error("单次最多上传 1000 条日志");
        }
        String appId = req.getAppId().trim();
        String employeeNo = req.getEmployeeNo() == null ? null : req.getEmployeeNo().trim();
        LocalDateTime now = LocalDateTime.now();
        List<LogRecord> records = new ArrayList<>();
        for (String content : sourceContents) {
            if (content == null || content.isBlank()) {
                continue;
            }
            LogRecord r = new LogRecord();
            r.setAppId(appId);
            if (employeeNo != null && !employeeNo.isBlank()) {
                r.setEmployeeNo(employeeNo);
            }
            r.setContent(content.trim());
            r.setDurationMs(extractDurationMs(content));
            r.setAck(0);
            r.setCreatedAt(now);
            records.add(r);
        }
        if (records.isEmpty()) {
            return Result.error("日志内容列表不能为空");
        }
        int accepted = logIngestService.ingestBatch(records);
        if (accepted <= 0) {
            return Result.error("日志写入繁忙，请稍后重试");
        }
        return Result.success(new BatchCreateLogResponse(accepted));
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
        Integer ack;
        if (req.getAck() != null && (req.getAck() == 0 || req.getAck() == 1)) {
            // 优先使用新参数 ack：0=未读，1=已读，null=全部
            ack = req.getAck();
        } else {
            // 兼容旧参数 unreadOnly
            boolean unreadOnly = req.getUnreadOnly() == null || req.getUnreadOnly();
            ack = unreadOnly ? 0 : null;
        }
        int page = req.getPage() == null || req.getPage() < 1 ? 1 : req.getPage();
        int size = req.getSize() == null || req.getSize() < 1 || req.getSize() > 100 ? 10 : req.getSize();
        int offset = (page - 1) * size;
        String appId = req.getAppId().trim();
        String employeeNo = req.getEmployeeNo() == null ? null : req.getEmployeeNo().trim();
        Long durationGt = req.getDurationGt() == null || req.getDurationGt() < 0 ? null : req.getDurationGt();
        LocalDateTime createdStart = req.getCreatedStartMs() == null ? null :
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(req.getCreatedStartMs()), ZoneId.systemDefault());
        LocalDateTime createdEnd = req.getCreatedEndMs() == null ? null :
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(req.getCreatedEndMs()), ZoneId.systemDefault());
        long total = logRecordMapper.countByFilter(appId, employeeNo, durationGt, createdStart, createdEnd, ack);
        List<LogRecord> list = logRecordMapper.selectList(appId, employeeNo, durationGt, createdStart, createdEnd, ack, offset, size);
        return Result.success(new PageResult<>(total, list));
    }

    private Long extractDurationMs(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(rawContent);
            Long fromCusp = extractHttpLatencyFromNode(node.get("cusp"));
            if (fromCusp != null) {
                return fromCusp;
            }
            Long fromRoot = extractHttpLatencyFromNode(node);
            if (fromRoot != null) {
                return fromRoot;
            }
        } catch (Exception ignored) {
            // fallback to regex below
        }
        Matcher matcher = HTTP_LATENCY_PATTERN.matcher(rawContent);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractHttpLatencyFromNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            if (node.isTextual()) {
                JsonNode cuspNode = OBJECT_MAPPER.readTree(node.asText());
                return extractHttpLatencyFromNode(cuspNode);
            }
            JsonNode latencyNode = node.get("httpLatency");
            if (latencyNode == null || latencyNode.isNull()) {
                return null;
            }
            if (latencyNode.isNumber()) {
                return latencyNode.asLong();
            }
            if (latencyNode.isTextual()) {
                return Long.parseLong(latencyNode.asText().trim());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static class UnreadListRequest {
        private String appId;
        private String employeeNo;
        private Boolean unreadOnly;
        private Integer ack;
        private Long durationGt;
        private Long createdStartMs;
        private Long createdEndMs;
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

        public Integer getAck() {
            return ack;
        }

        public void setAck(Integer ack) {
            this.ack = ack;
        }

        public Long getDurationGt() {
            return durationGt;
        }

        public void setDurationGt(Long durationGt) {
            this.durationGt = durationGt;
        }

        public String getEmployeeNo() {
            return employeeNo;
        }

        public void setEmployeeNo(String employeeNo) {
            this.employeeNo = employeeNo;
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

        public Long getCreatedStartMs() {
            return createdStartMs;
        }

        public void setCreatedStartMs(Long createdStartMs) {
            this.createdStartMs = createdStartMs;
        }

        public Long getCreatedEndMs() {
            return createdEndMs;
        }

        public void setCreatedEndMs(Long createdEndMs) {
            this.createdEndMs = createdEndMs;
        }
    }

    public static class CreateLogRequest {
        private String appId;
        private String employeeNo;
        private String raw;
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

        public String getEmployeeNo() {
            return employeeNo;
        }

        public void setEmployeeNo(String employeeNo) {
            this.employeeNo = employeeNo;
        }

        public String getRaw() {
            return raw;
        }

        public void setRaw(String raw) {
            this.raw = raw;
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

    public static class BatchCreateLogRequest {
        private String appId;
        private String employeeNo;
        private List<String> raws;
        private List<String> contents;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public List<String> getContents() {
            return contents;
        }

        public void setContents(List<String> contents) {
            this.contents = contents;
        }

        public String getEmployeeNo() {
            return employeeNo;
        }

        public void setEmployeeNo(String employeeNo) {
            this.employeeNo = employeeNo;
        }

        public List<String> getRaws() {
            return raws;
        }

        public void setRaws(List<String> raws) {
            this.raws = raws;
        }
    }

    public static class BatchCreateLogResponse {
        private int insertedCount;

        public BatchCreateLogResponse() {
        }

        public BatchCreateLogResponse(int insertedCount) {
            this.insertedCount = insertedCount;
        }

        public int getInsertedCount() {
            return insertedCount;
        }

        public void setInsertedCount(int insertedCount) {
            this.insertedCount = insertedCount;
        }
    }
}

