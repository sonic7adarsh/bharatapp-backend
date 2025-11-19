package com.bharatshop.web;

import com.bharatshop.service.MediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bharatshop.error.NotFoundException;

import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final MediaService mediaService = new MediaService();

    @Value("${aws.s3.bucket:demo-bucket}")
    private String bucket;

    @Value("${aws.region:ap-south-1}")
    private String region;

    @PostMapping("/sign-upload")
    public ResponseEntity<?> presign(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                     @RequestBody Map<String, Object> body) {
        String folder = body != null ? (String) body.getOrDefault("folder", "uploads") : "uploads";
        String contentType = body != null ? (String) body.getOrDefault("contentType", "application/octet-stream") : "application/octet-stream";
        log.info("Media presign requested: tenant={} folder={} contentType={} bucket={} region={}", tenant, folder, contentType, bucket, region);
        Map<String, Object> resp = mediaService.presign(bucket, region, folder, contentType);
        return ResponseEntity.ok(resp);
    }

    @PostMapping
    public ResponseEntity<?> finalizeUpload(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                            @RequestBody Map<String, Object> body) {
        String key = (String) body.getOrDefault("key", "");
        long size = body.get("size") instanceof Number ? ((Number) body.get("size")).longValue() : 0L;
        String contentType = (String) body.getOrDefault("contentType", "application/octet-stream");
        log.info("Media finalize requested: tenant={} key={} size={} contentType={}", tenant, key, size, contentType);
        Map<String, Object> resp = mediaService.finalize(bucket, region, key, size, contentType);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id,
                                    @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        log.info("Media delete requested: tenant={} id={} ", tenant, id);
        boolean ok = mediaService.delete(id);
        if (ok) return ResponseEntity.ok(Map.of("ok", true));
        throw new NotFoundException("Not found");
    }
}