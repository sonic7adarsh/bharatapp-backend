package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BulkUploadService {
    private static final Logger log = LoggerFactory.getLogger(BulkUploadService.class);

    public static class JobStatus {
        public String jobId;
        public String status; // queued|processing|completed|failed
        public String startedAt;
        public String completedAt;
        public Map<String, Integer> stats = new LinkedHashMap<>();
        public String message;
    }

    public static class JobError {
        public int row;
        public String column;
        public String code;
        public String message;
    }

    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();
    private final Map<String, List<JobError>> jobErrors = new ConcurrentHashMap<>();

    public JobStatus createJob(boolean dryRun, String mode, String defaultCurrency, Double defaultTaxRate, String source) {
        String jobId = "job_" + UUID.randomUUID();
        JobStatus js = new JobStatus();
        js.jobId = jobId;
        js.status = dryRun ? "completed" : "queued";
        js.startedAt = Instant.now().toString();
        js.stats.put("total", 0);
        js.stats.put("created", 0);
        js.stats.put("updated", 0);
        js.stats.put("unchanged", 0);
        js.stats.put("invalid", 0);

        if (dryRun) {
            js.completedAt = Instant.now().toString();
            js.message = "Dry run completed: no data persisted";
        }
        jobs.put(jobId, js);
        jobErrors.put(jobId, new ArrayList<>());
        log.info("Bulk upload job created: jobId={} dryRun={} mode={} source={}", jobId, dryRun, mode, source);
        return js;
    }

    public JobStatus getStatus(String jobId) { return jobs.get(jobId); }

    public List<JobError> getErrors(String jobId) { return jobErrors.getOrDefault(jobId, List.of()); }

    public JobStatus cancel(String jobId) {
        JobStatus js = jobs.get(jobId);
        if (js == null) return null;
        js.status = "failed"; // keep to defined statuses; failed due to cancel
        js.completedAt = Instant.now().toString();
        js.message = "Job cancelled";
        log.info("Bulk upload job cancelled: jobId={}", jobId);
        return js;
    }
}