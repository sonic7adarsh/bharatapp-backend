package com.bharatshop.web;

import com.bharatshop.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {
    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    @PostMapping("/events")
    public ResponseEntity<?> ingest(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                    @RequestBody Map<String, Object> body,
                                    HttpServletRequest request) {
        String name = body != null ? String.valueOf(body.get("name")) : null;
        if (name == null || name.isBlank() || "null".equalsIgnoreCase(name)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "code", "VALIDATION_ERROR",
                    "message", "Event 'name' is required"
            ));
        }

        Object payload = body.get("payload");
        Long timestamp = null;
        try {
            Object t = body.get("timestamp");
            if (t instanceof Number) timestamp = ((Number) t).longValue();
            else if (t instanceof String && !((String) t).isBlank()) timestamp = Long.parseLong((String) t);
        } catch (Exception ignored) {}
        if (timestamp == null) timestamp = Instant.now().toEpochMilli();

        var principal = UserPrincipal.current();
        String userId = principal != null ? principal.getUserId() : null;
        String role = principal != null ? principal.getRole() : null;

        String eventId = "evt_" + UUID.randomUUID();
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String requestId = request.getHeader("X-Request-Id");

        // Enrich and log the event. Persistence can be added later.
        log.info("Event ingested: id={} name={} tenant={} userId={} role={} ip={} ua={} ts={} payloadPresent={}",
                eventId, name, tenant, userId, role, ip, userAgent, timestamp, payload != null);

        return ResponseEntity.ok(Map.of("ok", true, "id", eventId));
    }
}