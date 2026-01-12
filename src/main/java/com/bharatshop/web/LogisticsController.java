package com.bharatshop.web;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.entity.DeliveryAttemptEntity;
import com.bharatshop.service.LogisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {
    private final LogisticsService logisticsService;

    public LogisticsController(LogisticsService logisticsService) { this.logisticsService = logisticsService; }

    private boolean ensureSellerOrAdmin() {
        com.bharatshop.security.UserPrincipal up = com.bharatshop.security.UserPrincipal.current();
        if (up == null) return false;
        String role = up.getRole();
        if (role == null) return false;
        role = role.toLowerCase();
        return role.equals("seller") || role.equals("vendor") || role.equals("admin");
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assign(@RequestBody Map<String, String> req, Authentication auth) {
        if (!ensureSellerOrAdmin()) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        String orderId = req.get("orderId");
        String storeId = req.get("storeId");
        if (orderId == null || storeId == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","orderId and storeId required"));
        }
        OrderDeliveryEntity delivery = logisticsService.assignRider(tenantId, orderId, storeId);
        if (delivery == null) {
            return ResponseEntity.ok(Map.of("status","no_rider","message","No rider available"));
        }
        return ResponseEntity.ok(Map.of(
                "status","assigned",
                "deliveryId", delivery.getId(),
                "riderId", delivery.getRiderId(),
                "otp", delivery.getOtp()
        ));
    }

    @PostMapping("/pickup")
    public ResponseEntity<?> pickup(@RequestBody Map<String, String> req) {
        if (!ensureSellerOrAdmin()) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String deliveryId = req.get("deliveryId");
        OrderDeliveryEntity d = logisticsService.markPickedUp(deliveryId);
        if (d == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid deliveryId"));
        return ResponseEntity.ok(Map.of("status", d.getStatus()));
    }

    @PostMapping("/complete")
    public ResponseEntity<?> complete(@RequestBody Map<String, String> req) {
        if (!ensureSellerOrAdmin()) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String deliveryId = req.get("deliveryId");
        String otp = req.get("otp");
        OrderDeliveryEntity d = logisticsService.completeWithOtp(deliveryId, otp);
        if (d == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid deliveryId"));
        return ResponseEntity.ok(Map.of("status", d.getStatus(), "reason", d.getFailureReason()));
    }

    @PostMapping("/out-for-delivery")
    public ResponseEntity<?> outForDelivery(@RequestBody Map<String, String> req) {
        if (!ensureSellerOrAdmin()) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String deliveryId = req.get("deliveryId");
        OrderDeliveryEntity d = logisticsService.markOutForDelivery(deliveryId);
        if (d == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid deliveryId"));
        return ResponseEntity.ok(Map.of("status", d.getStatus()));
    }

    @PostMapping("/attempt")
    public ResponseEntity<?> attempt(@RequestBody Map<String, String> req) {
        if (!ensureSellerOrAdmin()) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        String deliveryId = req.get("deliveryId");
        String status = req.getOrDefault("status", "failed");
        String note = req.getOrDefault("note", "");
        if (deliveryId == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","deliveryId required"));
        }
        java.util.Set<String> allowed = java.util.Set.of("failed", "success");
        if (!allowed.contains(status.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","invalid status"));
        }
        DeliveryAttemptEntity a = logisticsService.recordAttempt(tenantId, deliveryId, status, note);
        return ResponseEntity.ok(Map.of(
                "status", a.getStatus(),
                "deliveryId", a.getDeliveryId(),
                "attemptId", a.getId()
        ));
    }

    @GetMapping("/attempts")
    public ResponseEntity<?> listAttempts(@RequestParam String deliveryId) {
        if (!ensureSellerOrAdmin()) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        java.util.List<DeliveryAttemptEntity> attempts = logisticsService.getAttempts(tenantId, deliveryId);
        return ResponseEntity.ok(Map.of("attempts", attempts));
    }

    @GetMapping("/delivery/{id}")
    public ResponseEntity<?> getDelivery(@PathVariable String id) {
        if (!ensureSellerOrAdmin()) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        if (tenantId == null) return ResponseEntity.status(401).body(Map.of("status","error","message","Tenant required"));
        
        return logisticsService.findByTenantIdAndId(tenantId, id)
            .map(delivery -> {
                java.util.List<DeliveryAttemptEntity> attempts = logisticsService.getAttempts(tenantId, id);
                return ResponseEntity.ok(Map.of(
                    "delivery", delivery,
                    "attempts", attempts
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}