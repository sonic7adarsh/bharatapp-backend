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

    // MVP Local Upload for Prescription/Images
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> localUpload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
             return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        try {
            String filename = java.util.UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(uploadDir)) {
                java.nio.file.Files.createDirectories(uploadDir);
            }
            java.nio.file.Path target = uploadDir.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Return relative URL (client should prepend base URL or we return full local URL)
            // Assuming frontend can handle /uploads/filename
            String url = "/uploads/" + filename;
            return ResponseEntity.ok(Map.of("url", url, "publicUrl", url));
        } catch (java.io.IOException e) {
            log.error("Failed to upload file", e);
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed"));
        }
    }

    @PostMapping("/sign-upload")
    public ResponseEntity<?> presign(@RequestBody Map<String, Object> body) {
        String folder = body != null ? (String) body.getOrDefault("folder", "uploads") : "uploads";
        String contentType = body != null ? (String) body.getOrDefault("contentType", "application/octet-stream") : "application/octet-stream";
        log.info("Media presign requested: folder={} contentType={} bucket={} region={}", folder, contentType, bucket, region);
        Map<String, Object> resp = mediaService.presign(bucket, region, folder, contentType);
        return ResponseEntity.ok(resp);
    }

    @PostMapping
    public ResponseEntity<?> finalizeUpload(@RequestBody Map<String, Object> body) {
        String key = (String) body.getOrDefault("key", "");
        long size = body.get("size") instanceof Number ? ((Number) body.get("size")).longValue() : 0L;
        String contentType = (String) body.getOrDefault("contentType", "application/octet-stream");
        log.info("Media finalize requested: key={} size={} contentType={}", key, size, contentType);
        Map<String, Object> resp = mediaService.finalize(bucket, region, key, size, contentType);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        log.info("Media delete requested: id={} ", id);
        boolean ok = mediaService.delete(id);
        if (ok) return ResponseEntity.ok(Map.of("ok", true));
        throw new NotFoundException("Not found");
    }
}