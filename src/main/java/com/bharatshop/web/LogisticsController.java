package com.bharatshop.web;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.entity.DeliveryAttemptEntity;
import com.bharatshop.service.LogisticsService;
import com.bharatshop.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
@PreAuthorize("hasRole('RIDER')")
public class LogisticsController {
    private final LogisticsService logisticsService;

    public LogisticsController(LogisticsService logisticsService) { this.logisticsService = logisticsService; }

    private void ensureAssignedRiderOrAdmin(String tenantId, String deliveryId) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) {
            throw new com.bharatshop.error.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized");
        }
        boolean isAdmin = up.hasRole("ADMIN");
        OrderDeliveryEntity delivery = logisticsService.findByTenantIdAndId(tenantId, deliveryId)
                .orElseThrow(() -> new com.bharatshop.error.ApiException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "Delivery not found"));
        if (!isAdmin) {
            String riderId = up.getPrincipal().toString();
            if (delivery.getRiderId() == null || !delivery.getRiderId().equals(riderId)) {
                throw new com.bharatshop.error.ApiException(HttpStatus.FORBIDDEN, "NOT_ASSIGNED_RIDER", "Only assigned rider can perform this action");
            }
        }
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> assign(@RequestBody Map<String, String> req, Authentication auth) {
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
                "deliveryId", delivery.getDeliveryId(),
                "riderId", delivery.getRiderId(),
                "otp", delivery.getOtp()
        ));
    }

    @PostMapping("/pickup")
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> pickup(@RequestBody Map<String, String> req) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        String deliveryId = req.get("deliveryId");
        ensureAssignedRiderOrAdmin(tenantId, deliveryId);
        OrderDeliveryEntity d = logisticsService.markPickedUp(deliveryId);
        if (d == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid deliveryId"));
        return ResponseEntity.ok(Map.of("status", d.getStatus()));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> complete(@RequestBody Map<String, String> req) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        String deliveryId = req.get("deliveryId");
        String otp = req.get("otp");
        ensureAssignedRiderOrAdmin(tenantId, deliveryId);
        OrderDeliveryEntity d = logisticsService.completeWithOtp(deliveryId, otp);
        if (d == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid deliveryId"));
        return ResponseEntity.ok(Map.of("status", d.getStatus(), "reason", d.getFailureReason()));
    }

    @PostMapping("/out-for-delivery")
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> outForDelivery(@RequestBody Map<String, String> req) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        String deliveryId = req.get("deliveryId");
        ensureAssignedRiderOrAdmin(tenantId, deliveryId);
        OrderDeliveryEntity d = logisticsService.markOutForDelivery(deliveryId);
        if (d == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid deliveryId"));
        return ResponseEntity.ok(Map.of("status", d.getStatus()));
    }

    @PostMapping("/attempt")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> attempt(@RequestBody Map<String, String> req) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        String deliveryId = req.get("deliveryId");
        String status = req.getOrDefault("status", "failed");
        String note = req.getOrDefault("note", "");
        if (deliveryId == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","deliveryId required"));
        }
        ensureAssignedRiderOrAdmin(tenantId, deliveryId); // assigned rider check; admins allowed but RBAC is rider-only
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
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> listAttempts(@RequestParam String deliveryId) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        java.util.List<DeliveryAttemptEntity> attempts = logisticsService.getAttempts(tenantId, deliveryId);
        return ResponseEntity.ok(Map.of("attempts", attempts));
    }

    @GetMapping("/delivery/{id}")
    @PreAuthorize("hasRole('RIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> getDelivery(@PathVariable String id) {
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