package com.bharatshop.web.admin;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.service.LogisticsService;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/logistics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogisticsController {
    private final LogisticsService logisticsService;
    private final OrderRepository orderRepository;

    public AdminLogisticsController(LogisticsService logisticsService, OrderRepository orderRepository) {
        this.logisticsService = logisticsService;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/assign")
    public ResponseEntity<?> assign(@RequestBody Map<String, String> req) {
        String tenant = TenantContext.getTenant();
        String orderId = req.get("orderId");
        String riderId = req.get("riderId"); // optional explicit assignment
        if (orderId == null || orderId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "orderId required"));
        }
        Optional<com.bharatshop.entity.OrderEntity> opt = orderRepository.findByTenantIdAndId(tenant, orderId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "ORDER_NOT_FOUND"));
        }
        String storeId = opt.get().getStoreId();
        OrderDeliveryEntity d = logisticsService.assignRiderAdmin(tenant, orderId, storeId, riderId);
        if (d == null) return ResponseEntity.status(409).body(Map.of("error", "NO_AVAILABLE_RIDERS"));
        return ResponseEntity.ok(Map.of("deliveryId", d.getId(), "riderId", d.getRiderId(), "otp", d.getOtp()));
    }

    @PostMapping("/unassign")
    public ResponseEntity<?> unassign(@RequestBody Map<String, String> req) {
        String tenant = TenantContext.getTenant();
        String deliveryId = req.get("deliveryId");
        if (deliveryId == null || deliveryId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "deliveryId required"));
        }
        OrderDeliveryEntity d = logisticsService.unassignRiderAdmin(tenant, deliveryId);
        return ResponseEntity.ok(Map.of("status", "unassigned", "deliveryId", d.getId()));
    }
}