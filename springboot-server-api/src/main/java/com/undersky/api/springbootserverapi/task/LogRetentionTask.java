package com.undersky.api.springbootserverapi.task;

import com.undersky.api.springbootserverapi.mapper.LogRecordMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 日志保留策略：仅保留近 1 个月日志。
 */
@Component
public class LogRetentionTask {

    private final LogRecordMapper logRecordMapper;

    public LogRetentionTask(LogRecordMapper logRecordMapper) {
        this.logRecordMapper = logRecordMapper;
    }

    @PostConstruct
    public void cleanupOnStartup() {
        cleanup();
    }

    @Scheduled(cron = "0 10 3 * * ?")
    public void cleanupDaily() {
        cleanup();
    }

    private void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(1);
        logRecordMapper.deleteOlderThan(cutoff);
    }
}

