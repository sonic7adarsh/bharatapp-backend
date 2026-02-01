package com.bharatshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MediaService {
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    public Map<String, Object> presign(String bucket, String region, String folder, String contentType) {
        String key = (folder != null && !folder.isBlank() ? folder.trim() + "/" : "") + UUID.randomUUID();
        String uploadUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key + "?presign=" + UUID.randomUUID();
        String publicUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        return Map.of(
                "method", "PUT",
                "uploadUrl", uploadUrl,
                "headers", Map.of("Content-Type", contentType == null ? "application/octet-stream" : contentType),
                "bucket", bucket,
                "region", region,
                "key", key,
                "expiresInSec", 300,
                "publicUrl", publicUrl
        );
    }

    public Map<String, Object> finalize(String bucket, String region, String key, long size, String contentType) {
        String id = "mid_" + UUID.randomUUID();
        String publicUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        Map<String, Object> meta = new ConcurrentHashMap<>();
        meta.put("id", id);
        meta.put("bucket", bucket);
        meta.put("region", region);
        meta.put("key", key);
        meta.put("size", size);
        meta.put("contentType", contentType);
        meta.put("publicUrl", publicUrl);
        meta.put("createdAt", Instant.now().toEpochMilli());
        store.put(id, meta);
        log.info("Media finalized: id={} key={} size={} contentType={}", id, key, size, contentType);
        return Map.of("id", id, "publicUrl", publicUrl);
    }

    public boolean delete(String id) {
        Map<String, Object> removed = store.remove(id);
        log.info("Media delete request: id={} removed={} ", id, removed != null);
        // In real impl, also delete S3 object and invalidate CDN.
        return removed != null;
    }
}