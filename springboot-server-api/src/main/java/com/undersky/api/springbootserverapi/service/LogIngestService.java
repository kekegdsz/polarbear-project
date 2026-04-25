package com.undersky.api.springbootserverapi.service;

import com.undersky.api.springbootserverapi.mapper.LogRecordMapper;
import com.undersky.api.springbootserverapi.model.entity.LogRecord;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 高吞吐日志写入：
 * 接口线程仅入队，后台单线程按批次落库。
 */
@Service
public class LogIngestService {

    private static final int QUEUE_CAPACITY = 50000;
    private static final int MAX_BATCH_SIZE = 500;
    private static final long POLL_TIMEOUT_MS = 50L;

    private final BlockingQueue<LogRecord> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final LogRecordMapper logRecordMapper;
    private Thread workerThread;

    public LogIngestService(LogRecordMapper logRecordMapper) {
        this.logRecordMapper = logRecordMapper;
    }

    @PostConstruct
    public void start() {
        running.set(true);
        workerThread = new Thread(this::runWorker, "log-ingest-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
        flushRemaining();
    }

    public boolean ingest(LogRecord record) {
        return queue.offer(record);
    }

    public int ingestBatch(List<LogRecord> records) {
        int accepted = 0;
        for (LogRecord r : records) {
            if (queue.offer(r)) {
                accepted++;
            } else {
                break;
            }
        }
        return accepted;
    }

    private void runWorker() {
        List<LogRecord> batch = new ArrayList<>(MAX_BATCH_SIZE);
        while (running.get()) {
            try {
                LogRecord first = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                batch.add(first);
                queue.drainTo(batch, MAX_BATCH_SIZE - 1);
                flush(batch);
                batch.clear();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // worker 不能中断，吞掉异常继续处理后续批次
            }
        }
    }

    private void flushRemaining() {
        List<LogRecord> batch = new ArrayList<>(MAX_BATCH_SIZE);
        while (!queue.isEmpty()) {
            queue.drainTo(batch, MAX_BATCH_SIZE);
            if (!batch.isEmpty()) {
                flush(batch);
                batch.clear();
            }
        }
    }

    private void flush(List<LogRecord> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        try {
            logRecordMapper.insertBatch(batch);
        } catch (Exception e) {
            // 批量失败降级逐条，避免整批丢失
            for (LogRecord r : batch) {
                try {
                    logRecordMapper.insert(r);
                } catch (Exception ignored) {
                    // 单条失败继续后续，避免阻塞
                }
            }
        }
    }
}

