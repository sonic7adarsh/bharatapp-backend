package com.bharatshop.service;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.RiderEntity;
import com.bharatshop.repository.OrderDeliveryRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.repository.RiderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@Transactional
public class AdminLogisticsOverrideTest {
    @Autowired LogisticsService logisticsService;
    @Autowired RiderRepository riderRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderDeliveryRepository orderDeliveryRepository;

    @BeforeEach
    void setup() {
        // Tenant setup removed
    }

    private RiderEntity mkRider() {
        RiderEntity r = new RiderEntity();
        r.setId(UUID.randomUUID().toString());
        // TenantId removed
        r.setName("Test Rider");
        r.setPhone("9999999999");
        r.setStatus("ONLINE");
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        return riderRepository.save(r);
    }

    private OrderEntity mkOrderReady(String storeId) {
        OrderEntity o = new OrderEntity();
        o.setId(UUID.randomUUID().toString());
        // TenantId removed
        o.setStoreId(storeId);
        o.setStatus("ready");
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return orderRepository.save(o);
    }

    @Test
    void adminAssignExplicitRider_andUnassign() {
        RiderEntity r = mkRider();
        OrderEntity o = mkOrderReady("store-1");
        OrderDeliveryEntity d = logisticsService.assignRiderAdmin(o.getId(), o.getStoreId(), r.getId());
        Assertions.assertNotNull(d);
        Assertions.assertEquals("RIDER_ASSIGNED", d.getStatus());
        Assertions.assertEquals(r.getId(), d.getRiderId());

        OrderDeliveryEntity unassigned = logisticsService.unassignRiderAdmin(d.getDeliveryId());
        Assertions.assertEquals("PENDING", unassigned.getStatus());
        Assertions.assertNull(unassigned.getRiderId());
        RiderEntity refreshed = riderRepository.findById(r.getId()).orElseThrow();
        Assertions.assertEquals("ONLINE", refreshed.getStatus());
    }

    @Test
    void adminAssign_offlineRider_shouldFail() {
        RiderEntity rOffline = mkRider();
        rOffline.setStatus("OFFLINE");
        riderRepository.save(rOffline);
        
        OrderEntity o = mkOrderReady("store-2");
        Assertions.assertThrows(com.bharatshop.error.ApiException.class, () ->
                logisticsService.assignRiderAdmin(o.getId(), o.getStoreId(), rOffline.getId()));
    }
}