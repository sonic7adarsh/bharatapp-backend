package com.bharatshop.web.seller;

import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerDashboardController {

    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;

    public SellerDashboardController(StoreRepository storeRepository, OrderRepository orderRepository) {
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        UserPrincipal principal = UserPrincipal.current();
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        List<StoreEntity> stores = storeRepository.findByOwnerId(principal.getUserId());
        
        int todayCount = 0;
        int pendingCount = 0;
        double earnings = 0.0;
        
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);

        for (StoreEntity store : stores) {
            List<OrderEntity> orders = orderRepository.findByStoreId(store.getId());
            for (OrderEntity order : orders) {
                // Today's orders
                if (order.getCreatedAt() != null && order.getCreatedAt().isAfter(startOfDay)) {
                    todayCount++;
                }
                
                // Pending orders
                if ("placed".equalsIgnoreCase(order.getStatus()) || "pending".equalsIgnoreCase(order.getStatus())) {
                    pendingCount++;
                }
                
                // Earnings (Completed orders)
                if ("delivered".equalsIgnoreCase(order.getStatus()) || "completed".equalsIgnoreCase(order.getStatus())) {
                    if (order.getTotal() != null) {
                        earnings += order.getTotal();
                    }
                }
            }
        }

        return ResponseEntity.ok(Map.of(
            "today", todayCount,
            "pending", pendingCount,
            "earnings", earnings
        ));
    }
}
