package com.bharatshop.service;

import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.OrderItemEntity;
import com.bharatshop.error.ApiException;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class SLAAndCancelRulesTest {
    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    // private final String tenant = "tenant-auto"; // Removed
    private final String userId = "user-1";

    @BeforeEach
    void setup() {
        // Tenant context removed
    }

    @Test
    void autoCancelAfterSellerAcceptanceWindow() {
        // Seed order placed with past sellerResponseDeadline
        OrderEntity e = new OrderEntity();
        e.setId(UUID.randomUUID().toString());
        // tenantId removed
        e.setUserId(userId);
        e.setStatus("placed");
        e.setCreatedAt(Instant.now().minusSeconds(3600));
        e.setSellerResponseDeadline(Instant.now().minusSeconds(60));
        orderRepository.save(e);

        OrderItemEntity oi = new OrderItemEntity();
        oi.setId(UUID.randomUUID().toString());
        oi.setOrderId(e.getId());
        oi.setProductId("prod-1");
        oi.setName("Item");
        oi.setPrice(100.0);
        oi.setQuantity(2);
        orderItemRepository.save(oi);

        List<com.bharatshop.domain.Order> list = orderService.listOrders(userId);
        com.bharatshop.domain.Order dto = list.stream().filter(o -> e.getId().equals(o.getId())).findFirst().orElse(null);
        assertThat(dto).isNotNull();
        assertThat(dto.getStatus()).isEqualTo("cancelled");
        assertThat(dto.getCancellationReason()).isEqualTo("auto_cancelled_no_response");
    }

    @Test
    void userCancelAllowedBeforeReady() {
        OrderEntity e = new OrderEntity();
        e.setId(UUID.randomUUID().toString());
        // tenantId removed
        e.setUserId(userId);
        e.setStatus("placed");
        e.setCreatedAt(Instant.now());
        orderRepository.save(e);

        com.bharatshop.domain.Order cancelled = orderService.cancelOrderByUser(userId, e.getId(), "user_cancel");
        assertThat(cancelled).isNotNull();
        assertThat(cancelled.getStatus()).isEqualTo("cancelled");
        assertThat(cancelled.getCancellationReason()).isEqualTo("user_cancel");
    }

    @Test
    void userCancelBlockedAfterReady() {
        OrderEntity e = new OrderEntity();
        e.setId(UUID.randomUUID().toString());
        // tenantId removed
        e.setUserId(userId);
        e.setStatus("ready");
        e.setCreatedAt(Instant.now());
        orderRepository.save(e);

        ApiException ex = assertThrows(ApiException.class, () -> orderService.cancelOrderByUser(userId, e.getId(), "user_cancel"));
        assertThat(ex.getStatus().value()).isEqualTo(409);
        assertThat(ex.getCode()).isEqualTo("CANCEL_NOT_ALLOWED");
    }
}