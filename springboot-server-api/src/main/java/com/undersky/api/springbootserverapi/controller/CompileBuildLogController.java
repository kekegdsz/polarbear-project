package com.undersky.api.springbootserverapi.controller;

import com.undersky.api.springbootserverapi.mapper.CompileBuildLogMapper;
import com.undersky.api.springbootserverapi.model.dto.PageResult;
import com.undersky.api.springbootserverapi.model.dto.Result;
import com.undersky.api.springbootserverapi.model.entity.CompileBuildLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gradle/CI 编译结果上报；列表查询需管理后台登录（同 /admin/** 拦截）
 */
@RestController
@RequestMapping("/admin/compile-logs")
public class CompileBuildLogController {

    private static final int MAX_MACHINE_LEN = 512;
    private static final int MAX_TASKS_LEN = 1024;
    private static final int MAX_PROJECT_KEY_LEN = 128;
    private static final int MAX_OS_USER_LEN = 128;

    private final CompileBuildLogMapper compileBuildLogMapper;

    public CompileBuildLogController(CompileBuildLogMapper compileBuildLogMapper) {
        this.compileBuildLogMapper = compileBuildLogMapper;
    }

    /**
     * 浏览器直接打开会使用 GET；本接口实际上报须 POST JSON。
     */
    @GetMapping("/report")
    public Result<Map<String, Object>> reportUsageHint() {
        Map<String, Object> hint = new LinkedHashMap<>();
        hint.put("message", "编译上报请使用 POST，Content-Type: application/json；浏览器地址栏只会发起 GET，无法落库。");
        hint.put("method", "POST");
        hint.put("requiredFields", List.of("appId", "durationMs", "startedAtMs", "endedAtMs"));
        hint.put("optionalFields", List.of("projectKey", "machine", "osUser", "tasks", "success"));
        return Result.success(hint);
    }

    /**
     * 上报一条编译记录（无需 admin token，与 /admin/logs 写入策略一致）
     */
    @PostMapping("/report")
    public Result<CompileBuildLog> report(@RequestBody ReportRequest req) {
        if (req == null || req.getAppId() == null || req.getAppId().isBlank()) {
            return Result.error("appId 不能为空");
        }
        if (req.getStartedAtMs() == null || req.getEndedAtMs() == null) {
            return Result.error("startedAtMs 与 endedAtMs 不能为空");
        }
        if (req.getDurationMs() == null || req.getDurationMs() < 0) {
            return Result.error("durationMs 需为大于等于 0 的整数");
        }
        if (req.getEndedAtMs() < req.getStartedAtMs()) {
            return Result.error("endedAtMs 不能早于 startedAtMs");
        }

        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime started = LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getStartedAtMs()), zone);
        LocalDateTime ended = LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getEndedAtMs()), zone);

        CompileBuildLog row = new CompileBuildLog();
        row.setAppId(req.getAppId().trim());
        row.setProjectKey(trimToNull(truncate(req.getProjectKey(), MAX_PROJECT_KEY_LEN)));
        row.setMachine(trimToNull(truncate(req.getMachine(), MAX_MACHINE_LEN)));
        row.setOsUser(trimToNull(truncate(req.getOsUser(), MAX_OS_USER_LEN)));
        row.setDurationMs(req.getDurationMs());
        row.setStartedAt(started);
        row.setEndedAt(ended);
        row.setTasks(trimToNull(truncate(req.getTasks(), MAX_TASKS_LEN)));
        row.setSuccess(Boolean.FALSE.equals(req.getSuccess()) ? 0 : 1);
        row.setCreatedAt(LocalDateTime.now());

        compileBuildLogMapper.insert(row);
        return Result.success(row);
    }

    @PostMapping("/list")
    public Result<PageResult<CompileBuildLog>> list(@RequestBody(required = false) ListRequest req) {
        if (req == null || req.getAppId() == null || req.getAppId().isBlank()) {
            return Result.error("appId 不能为空");
        }
        int page = req.getPage() == null || req.getPage() < 1 ? 1 : req.getPage();
        int size = req.getSize() == null || req.getSize() < 1 || req.getSize() > 100 ? 20 : req.getSize();
        int offset = (page - 1) * size;
        String appId = req.getAppId().trim();
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime createdStart = req.getCreatedStartMs() == null ? null :
                LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getCreatedStartMs()), zone);
        LocalDateTime createdEnd = req.getCreatedEndMs() == null ? null :
                LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getCreatedEndMs()), zone);
        long total = compileBuildLogMapper.countByFilter(appId, createdStart, createdEnd);
        List<CompileBuildLog> list = compileBuildLogMapper.selectList(appId, createdStart, createdEnd, offset, size);
        return Result.success(new PageResult<>(total, list));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static class ReportRequest {
        private String appId;
        private String projectKey;
        private String machine;
        private String osUser;
        private Long durationMs;
        private Long startedAtMs;
        private Long endedAtMs;
        private String tasks;
        private Boolean success;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getProjectKey() {
            return projectKey;
        }

        public void setProjectKey(String projectKey) {
            this.projectKey = projectKey;
        }

        public String getMachine() {
            return machine;
        }

        public void setMachine(String machine) {
            this.machine = machine;
        }

        public String getOsUser() {
            return osUser;
        }

        public void setOsUser(String osUser) {
            this.osUser = osUser;
        }

        public Long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Long durationMs) {
            this.durationMs = durationMs;
        }

        public Long getStartedAtMs() {
            return startedAtMs;
        }

        public void setStartedAtMs(Long startedAtMs) {
            this.startedAtMs = startedAtMs;
        }

        public Long getEndedAtMs() {
            return endedAtMs;
        }

        public void setEndedAtMs(Long endedAtMs) {
            this.endedAtMs = endedAtMs;
        }

        public String getTasks() {
            return tasks;
        }

        public void setTasks(String tasks) {
            this.tasks = tasks;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }
    }

    public static class ListRequest {
        private String appId;
        private Integer page;
        private Integer size;
        private Long createdStartMs;
        private Long createdEndMs;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
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
}
