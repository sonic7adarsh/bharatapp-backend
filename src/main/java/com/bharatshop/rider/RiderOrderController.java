package com.bharatshop.rider;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.service.LogisticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rider")
@PreAuthorize("hasRole('RIDER')")
public class RiderOrderController {

    private final LogisticsService logisticsService;

    public RiderOrderController(LogisticsService logisticsService) { this.logisticsService = logisticsService; }

    private void ensureAssignedRiderOrAdmin(String tenantId, String deliveryId) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) {
            throw new com.bharatshop.error.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized");
        }
        boolean isAdmin = up.hasRole("ADMIN");
        OrderDeliveryEntity delivery = logisticsService.findByTenantIdAndId(tenantId, deliveryId)
                .orElseThrow(() -> new com.bharatshop.error.ApiException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "Delivery not found"));
        if (!isAdmin) {
            String riderId = up.getUserId();
            if (delivery.getRiderId() == null || !delivery.getRiderId().equals(riderId)) {
                throw new com.bharatshop.error.ApiException(HttpStatus.FORBIDDEN, "NOT_ASSIGNED_RIDER", "Only assigned rider can perform this action");
            }
        }
    }

    @PostMapping("/orders/{id}/pickup")
    public ResponseEntity<?> pickup(@PathVariable("id") String deliveryId) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        ensureAssignedRiderOrAdmin(tenantId, deliveryId);
        OrderDeliveryEntity d = logisticsService.markPickedUp(deliveryId);
        if (d == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid deliveryId"));
        return ResponseEntity.ok(Map.of("status", d.getStatus()));
    }

    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<?> deliver(@PathVariable("id") String deliveryId, @RequestBody(required = false) Map<String, String> body) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        ensureAssignedRiderOrAdmin(tenantId, deliveryId);
        String otp = body != null ? body.get("otp") : null;
        OrderDeliveryEntity d = logisticsService.completeWithOtp(deliveryId, otp);
        if (d == null) return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid deliveryId"));
        return ResponseEntity.ok(Map.of("status", d.getStatus(), "reason", d.getFailureReason()));
    }
}