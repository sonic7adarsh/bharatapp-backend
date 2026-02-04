package com.bharatshop.rider;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.entity.RiderEntity;
import com.bharatshop.entity.RiderStatusEntity;
import com.bharatshop.entity.RiderEarningEntity;
import com.bharatshop.repository.OrderDeliveryRepository;
import com.bharatshop.repository.RiderRepository;
import com.bharatshop.repository.RiderStatusRepository;
import com.bharatshop.repository.RiderEarningRepository;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.security.JwtService;
import com.bharatshop.service.LogisticsService;
import com.bharatshop.service.UserRoleService;
import com.bharatshop.repository.UserRepository;
import com.bharatshop.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController("riderModuleController")
@RequestMapping("/api/rider")
public class RiderController {

    private final RiderRepository riderRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final RiderStatusRepository riderStatusRepository;
    private final RiderEarningRepository riderEarningRepository;
    private final LogisticsService logisticsService;
    private final JwtService jwtService;
    private final UserRoleService userRoleService;
    private final UserRepository userRepository;

    public RiderController(RiderRepository riderRepository,
                           OrderDeliveryRepository orderDeliveryRepository,
                           RiderStatusRepository riderStatusRepository,
                           RiderEarningRepository riderEarningRepository,
                           LogisticsService logisticsService,
                           JwtService jwtService,
                           UserRoleService userRoleService,
                           UserRepository userRepository) {
        this.riderRepository = riderRepository;
        this.orderDeliveryRepository = orderDeliveryRepository;
        this.riderStatusRepository = riderStatusRepository;
        this.riderEarningRepository = riderEarningRepository;
        this.logisticsService = logisticsService;
        this.jwtService = jwtService;
        this.userRoleService = userRoleService;
        this.userRepository = userRepository;
    }

    // tenantId helper removed


    private String currentRiderId() {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) {
            throw new com.bharatshop.error.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized");
        }
        return up.getUserId();
    }

    @PostMapping("/onboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> onboard(@RequestBody Map<String, String> body) {
        String riderId = currentRiderId();
        String name = body != null ? body.getOrDefault("name", "") : "";
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","name required"));
        }
        // Derive phone from authenticated user's persisted record (source of truth)
        UserEntity user = userRepository.findById(riderId)
                .orElseThrow(() -> new com.bharatshop.error.ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DATA_INTEGRITY", "Authenticated user not found"));
        String phone = user.getPhone();
        if (phone == null || phone.isBlank()) {
            throw new com.bharatshop.error.ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DATA_INTEGRITY", "Phone missing for authenticated user");
        }
        RiderEntity rider = riderRepository.findById(riderId).orElseGet(RiderEntity::new);
        rider.setId(riderId);
        rider.setName(name);
        rider.setPhone(phone);
        if (rider.getStatus() == null || rider.getStatus().isBlank()) rider.setStatus("OFFLINE");
        riderRepository.save(rider);
        // Role assignment point: ensure RIDER role exists and is active; idempotent
        java.util.List<String> rolesBefore = userRoleService.getUserRoles(riderId);
        boolean hadRider = rolesBefore != null && rolesBefore.stream().anyMatch(r -> "RIDER".equalsIgnoreCase(r));
        if (!hadRider) {
            userRoleService.addRoleToUser(riderId, "RIDER");
        }
        // Switch active role to RIDER
        userRoleService.switchUserRole(riderId, "RIDER");
        // Compose allowed roles list
        java.util.List<String> allowedRoles = userRoleService.getUserRoles(riderId);
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            allowedRoles = java.util.List.of("CUSTOMER", "RIDER");
        }
        // Issue new JWT with active_role=RIDER and full roles
        String token = jwtService.generateTokenWithRoles(
                rider.getId(),
                rider.getName() == null ? "Rider" : rider.getName(),
                "RIDER",
                "RIDER",
                allowedRoles
        );
        return ResponseEntity.ok(Map.of(
                "id", rider.getId(),
                "name", rider.getName(),
                "phone", rider.getPhone(),
                "status", rider.getStatus(),
                "token", token,
                "active_role", "RIDER",
                "allowed_roles", allowedRoles
        ));
    }

    @PostMapping("/status")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> setStatus(@RequestBody Map<String, String> body) {
        String riderId = currentRiderId();
        String status = body != null ? body.getOrDefault("status", "").toUpperCase() : "";
        Set<String> allowed = Set.of("ONLINE", "OFFLINE");
        if (!allowed.contains(status)) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","status must be ONLINE or OFFLINE"));
        }
        RiderEntity rider = riderRepository.findById(riderId).orElseGet(() -> {
            RiderEntity r = new RiderEntity();
            r.setId(riderId);
            r.setStatus("OFFLINE");
            return r;
        });
        rider.setStatus(status);
        riderRepository.save(rider);

        RiderStatusEntity rs = new RiderStatusEntity();
        rs.setId(java.util.UUID.randomUUID().toString());
        rs.setRiderId(riderId);
        rs.setStatus(status);
        rs.setTs(java.time.Instant.now());
        riderStatusRepository.save(rs);

        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/orders/available")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> listAvailable() {
        List<OrderDeliveryEntity> deliveries = orderDeliveryRepository.findByStatus("READY");
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrderDeliveryEntity d : deliveries) {
            out.add(Map.of(
                    "deliveryId", d.getDeliveryId(),
                    "orderId", d.getOrderId(),
                    "storeId", d.getStoreId(),
                    "status", d.getStatus()
            ));
        }
        return ResponseEntity.ok(Map.of("deliveries", out));
    }

    @PostMapping("/orders/{deliveryId}/accept")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> acceptOrder(@PathVariable("deliveryId") String deliveryId) {
        logisticsService.accept(deliveryId);
        return ResponseEntity.ok(Map.of("status", "RIDER_ASSIGNED"));
    }

    @GetMapping("/orders/active")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> listMyOrders() {
        String riderId = currentRiderId();
        List<OrderDeliveryEntity> deliveries = orderDeliveryRepository.findByRiderIdAndStatusIn(
                riderId, java.util.List.of("RIDER_ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY")
        );
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrderDeliveryEntity d : deliveries) {
            out.add(Map.of(
                    "deliveryId", d.getDeliveryId(),
                    "orderId", d.getOrderId(),
                    "storeId", d.getStoreId(),
                    "status", d.getStatus()
            ));
        }
        return ResponseEntity.ok(Map.of("deliveries", out));
    }

    @PostMapping("/orders/{deliveryId}/pickup")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> pickup(@PathVariable("deliveryId") String deliveryId) {
        String riderId = currentRiderId();
        OrderDeliveryEntity delivery = orderDeliveryRepository.findById(deliveryId)
                .orElse(null);
        if (delivery == null) {
            return ResponseEntity.status(404).body(Map.of("status","error","message","Delivery not found"));
        }
        if (delivery.getRiderId() == null || !delivery.getRiderId().equals(riderId)) {
            return ResponseEntity.status(403).body(Map.of("status","error","message","Not assigned to this delivery"));
        }
        if (!"RIDER_ASSIGNED".equalsIgnoreCase(delivery.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("status","error","message","Invalid transition"));
        }
        OrderDeliveryEntity d = logisticsService.markPickedUp(delivery.getDeliveryId());
        return ResponseEntity.ok(Map.of("status", d.getStatus()));
    }

    @PostMapping("/orders/{deliveryId}/start-delivery")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> startDelivery(@PathVariable("deliveryId") String deliveryId) {
        String riderId = currentRiderId();
        OrderDeliveryEntity delivery = orderDeliveryRepository.findById(deliveryId)
                .orElse(null);
        if (delivery == null) {
            return ResponseEntity.status(404).body(Map.of("status","error","message","Delivery not found"));
        }
        if (delivery.getRiderId() == null || !delivery.getRiderId().equals(riderId)) {
            return ResponseEntity.status(403).body(Map.of("status","error","message","Not assigned to this delivery"));
        }
        if (!"PICKED_UP".equalsIgnoreCase(delivery.getStatus())) {
            return ResponseEntity.status(409).body(Map.of("status","error","message","Invalid transition"));
        }
        OrderDeliveryEntity d = logisticsService.startDelivery(delivery.getDeliveryId());
        return ResponseEntity.ok(Map.of("status", d.getStatus(), "otp_sent", true));
    }

    @PostMapping("/orders/{deliveryId}/deliver")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> deliver(@PathVariable("deliveryId") String deliveryId, @RequestBody(required = false) Map<String, String> body) {
        String riderId = currentRiderId();
        String otp = body != null ? body.get("otp") : null;
        if (otp == null || !otp.matches("^\\d{6}$")) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid OTP format"));
        }
        OrderDeliveryEntity delivery = orderDeliveryRepository.findById(deliveryId)
                .orElse(null);
        if (delivery == null) {
            return ResponseEntity.status(404).body(Map.of("status","error","message","Delivery not found"));
        }
        if (delivery.getRiderId() == null || !delivery.getRiderId().equals(riderId)) {
            return ResponseEntity.status(403).body(Map.of("status","error","message","Not assigned to this delivery"));
        }
        String cur = delivery.getStatus() == null ? "" : delivery.getStatus().toUpperCase();
        if (!"OUT_FOR_DELIVERY".equals(cur)) {
            return ResponseEntity.status(409).body(Map.of("status","error","message","Invalid transition"));
        }
        OrderDeliveryEntity d = logisticsService.completeWithOtp(delivery.getDeliveryId(), otp);
        // record earning on successful delivery
        if (d != null && "DELIVERED".equalsIgnoreCase(d.getStatus())) {
            boolean exists = riderEarningRepository
                    .findFirstByDeliveryId(d.getDeliveryId())
                    .isPresent();
            if (!exists) {
                RiderEarningEntity earning = new RiderEarningEntity();
                earning.setId(java.util.UUID.randomUUID().toString());
                // tenantId removed
                earning.setRiderId(riderId);
                earning.setOrderId(d.getOrderId());
                earning.setDeliveryId(d.getDeliveryId());
                earning.setAmount(new BigDecimal("50.00")); // basic per-order earning
                riderEarningRepository.save(earning);
            }
        }
        return ResponseEntity.ok(Map.of("status", d.getStatus()));
    }

    @GetMapping("/orders/history")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<?> history() {
        String riderId = currentRiderId();
        List<OrderDeliveryEntity> deliveries = orderDeliveryRepository.findCompleted(riderId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrderDeliveryEntity d : deliveries) {
            out.add(Map.of(
                    "deliveryId", d.getDeliveryId(),
                    "orderId", d.getOrderId(),
                    "storeId", d.getStoreId(),
                    "status", d.getStatus()
            ));
        }
        return ResponseEntity.ok(Map.of("deliveries", out));
    }

    // COPY-PASTE (EXACT) - Active and Completed endpoints
    @GetMapping("/active")
    @PreAuthorize("hasRole('RIDER')")
    public Map<String, Object> active() {
      return Map.of(
        "deliveries",
        orderDeliveryRepository.findActive(currentRiderId())
      );
    }

    @GetMapping("/completed")
    @PreAuthorize("hasRole('RIDER')")
    public Map<String, Object> completed() {
      return Map.of(
        "deliveries",
        orderDeliveryRepository.findCompleted(currentRiderId())
      );
    }
}