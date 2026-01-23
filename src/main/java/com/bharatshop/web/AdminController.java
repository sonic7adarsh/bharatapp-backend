package com.bharatshop.web;

import com.bharatshop.domain.Order;
import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.entity.OrderEntity;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.service.LogisticsService;
import com.bharatshop.service.OrderService;
import com.bharatshop.service.UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final OrderService orderService;
    private final LogisticsService logisticsService;
    private final UserRoleService userRoleService;
    private final OrderRepository orderRepository;

    public AdminController(OrderService orderService,
                           LogisticsService logisticsService,
                           UserRoleService userRoleService,
                           OrderRepository orderRepository) {
        this.orderService = orderService;
        this.logisticsService = logisticsService;
        this.userRoleService = userRoleService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrder(@RequestHeader("X-Tenant-Domain") String tenant,
                                      @PathVariable String orderId) {
        Optional<OrderEntity> opt = orderRepository.findByTenantIdAndId(tenant, orderId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "ORDER_NOT_FOUND"));
        Order dto = orderService.toDto(opt.get());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@RequestHeader("X-Tenant-Domain") String tenant,
                                         @PathVariable String orderId,
                                         @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "admin_cancelled") : "admin_cancelled";
        log.info("Admin cancel order: tenant={} orderId={} reason={}", tenant, orderId, reason);
        Order dto = orderService.cancelOrderByAdmin(tenant, orderId, reason);
        return ResponseEntity.ok(Map.of("status", dto.getStatus(), "orderId", dto.getId()));
    }

    @PostMapping("/orders/{orderId}/reassign")
    public ResponseEntity<?> reassignRider(@RequestHeader("X-Tenant-Domain") String tenant,
                                           @PathVariable String orderId) {
        Optional<OrderEntity> opt = orderRepository.findByTenantIdAndId(tenant, orderId);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "ORDER_NOT_FOUND"));
        String storeId = opt.get().getStoreId();
        OrderDeliveryEntity d = logisticsService.assignRider(tenant, orderId, storeId);
        if (d == null) return ResponseEntity.status(409).body(Map.of("error", "NO_AVAILABLE_RIDERS"));
        return ResponseEntity.ok(Map.of("deliveryId", d.getDeliveryId(), "riderId", d.getRiderId()));
    }

    @PostMapping("/grant-role")
    public ResponseEntity<?> grantRole(@RequestBody Map<String, String> body) {
        String userId = body != null ? body.get("userId") : null;
        String role = body != null ? body.get("role") : null;
        if (userId == null || role == null || role.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_PAYLOAD"));
        }
        log.info("Admin grant role: userId={} role={}", userId, role);
        userRoleService.addRoleToUser(userId, role);
        var roles = userRoleService.getUserRoles(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "roles", roles));
    }
}