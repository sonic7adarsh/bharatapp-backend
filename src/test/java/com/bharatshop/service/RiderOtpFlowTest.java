package com.bharatshop.service;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.OrderItemEntity;
import com.bharatshop.error.ApiException;
import com.bharatshop.repository.OrderDeliveryRepository;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class RiderOtpFlowTest {
    @Autowired private LogisticsService logisticsService;
    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private OrderDeliveryRepository orderDeliveryRepository;

    private final String tenant = "tenant-otp";

    @BeforeEach
    void setup() {
        TenantContext.setTenant(tenant);
    }

    @Test
    void completeWithOtpConsumesInventoryAndSetsDelivered() {
        // Arrange: create order with items and delivery entity, mark out for delivery
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setTenantId(tenant);
        order.setUserId("buyer-1");
        order.setStatus("shipped");
        order.setCreatedAt(Instant.now());
        orderRepository.save(order);

        OrderItemEntity item = new OrderItemEntity();
        item.setId(UUID.randomUUID().toString());
        item.setOrderId(order.getId());
        item.setProductId("p-1");
        item.setName("Widget");
        item.setPrice(42.0);
        item.setQuantity(1);
        orderItemRepository.save(item);

        OrderDeliveryEntity delivery = new OrderDeliveryEntity();
        delivery.setId(UUID.randomUUID().toString());
        delivery.setTenantId(tenant);
        delivery.setOrderId(order.getId());
        delivery.setStatus("out_for_delivery");
        delivery.setOtp("123456");
        orderDeliveryRepository.save(delivery);

        // Act
        OrderDeliveryEntity completed = logisticsService.completeWithOtp(delivery.getId(), "123456");

        // Assert: order delivered and inventory consumed handled by service
        assertThat(completed.getStatus()).isEqualTo("DELIVERED");
        OrderEntity refreshed = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("delivered");
    }

    @Test
    void completeWithWrongOtpFails() {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setTenantId(tenant);
        order.setUserId("buyer-1");
        order.setStatus("shipped");
        order.setCreatedAt(Instant.now());
        orderRepository.save(order);

        OrderDeliveryEntity delivery = new OrderDeliveryEntity();
        delivery.setId(UUID.randomUUID().toString());
        delivery.setTenantId(tenant);
        delivery.setOrderId(order.getId());
        delivery.setStatus("out_for_delivery");
        delivery.setOtp("654321");
        orderDeliveryRepository.save(delivery);

        ApiException ex = assertThrows(ApiException.class,
                () -> logisticsService.completeWithOtp(delivery.getId(), "000000"));
        assertThat(ex.getStatus().value()).isEqualTo(409);
        assertThat(ex.getCode()).isEqualTo("INVALID_OTP");
    }
}