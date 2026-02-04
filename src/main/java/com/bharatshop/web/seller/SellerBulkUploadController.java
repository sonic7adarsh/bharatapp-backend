package com.bharatshop.web.seller;

import com.bharatshop.security.UserPrincipal;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.service.BulkUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/products/bulk-upload")
@PreAuthorize("hasRole('SELLER')")
public class SellerBulkUploadController {
    private static final Logger log = LoggerFactory.getLogger(SellerBulkUploadController.class);
    private final BulkUploadService bulkUploadService;

    public SellerBulkUploadController(BulkUploadService bulkUploadService) { this.bulkUploadService = bulkUploadService; }

    // RBAC via @PreAuthorize; avoid manual auth checks

    // POST /api/seller/products/bulk-upload (multipart or JSON)
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<?> upload(@RequestParam(required = false) String mode,
                                    @RequestParam(required = false) Boolean dryRun,
                                    @RequestParam(required = false) String defaultCurrency,
                                    @RequestParam(required = false) Double defaultTaxRate,
                                    @RequestPart(required = false, name = "file") MultipartFile file,
                                    @RequestBody(required = false) Map<String, Object> body) {
        boolean isDry = dryRun != null && dryRun;
        String effectiveMode = StringUtils.hasText(mode) ? mode : "upsert";
        String source;
        if (file != null && !file.isEmpty()) {
            source = "multipart:" + file.getOriginalFilename();
        } else {
            String key = body != null ? (String) body.get("key") : null;
            source = StringUtils.hasText(key) ? ("s3:" + key) : "unknown";
        }
        log.info("Bulk upload request: mode={} dryRun={} source={} ", effectiveMode, isDry, source);
        var job = bulkUploadService.createJob(isDry, effectiveMode, defaultCurrency, defaultTaxRate, source);
        if (isDry) {
            return ResponseEntity.ok(Map.of(
                    "dryRun", true,
                    "totalRows", 0,
                    "validRows", 0,
                    "invalidRows", 0,
                    "errors", List.of()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "jobId", job.jobId,
                "status", job.status,
                "acceptedRows", 0
        ));
    }

    // GET /api/seller/products/bulk-upload/{jobId}
    @GetMapping("/{jobId}")
    public ResponseEntity<?> status(@PathVariable String jobId) {
        var js = bulkUploadService.getStatus(jobId);
        if (js == null) throw new NotFoundException("Job not found");
        return ResponseEntity.ok(Map.of(
                "jobId", js.jobId,
                "status", js.status,
                "startedAt", js.startedAt,
                "completedAt", js.completedAt,
                "stats", js.stats
        ));
    }

    // GET /api/seller/products/bulk-upload/{jobId}/errors
    @GetMapping("/{jobId}/errors")
    public ResponseEntity<?> errors(@PathVariable String jobId) {
        List<BulkUploadService.JobError> errs = bulkUploadService.getErrors(jobId);
        if (errs == null) throw new NotFoundException("Job not found");
        return ResponseEntity.ok(errs);
    }

    // Optional cancel: behaves consistently with pipeline semantics
    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> cancel(@PathVariable String jobId) {
        var js = bulkUploadService.cancel(jobId);
        if (js == null) throw new NotFoundException("Job not found");
        return ResponseEntity.ok(Map.of(
                "jobId", js.jobId,
                "status", js.status,
                "completedAt", js.completedAt,
                "message", js.message
        ));
    }
}