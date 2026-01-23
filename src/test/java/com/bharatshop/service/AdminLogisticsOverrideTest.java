package com.bharatshop.service;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.RiderEntity;
import com.bharatshop.repository.OrderDeliveryRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.repository.RiderRepository;
import com.bharatshop.tenant.TenantContext;
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
    void setup() { TenantContext.setTenant("default"); }

    private RiderEntity mkRider(String tenant) {
        RiderEntity r = new RiderEntity();
        r.setId(UUID.randomUUID().toString());
        r.setTenantId(tenant);
        r.setName("Test Rider");
        r.setPhone("9999999999");
        r.setStatus("ONLINE");
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        return riderRepository.save(r);
    }

    private OrderEntity mkOrderReady(String tenant, String storeId) {
        OrderEntity o = new OrderEntity();
        o.setId(UUID.randomUUID().toString());
        o.setTenantId(tenant);
        o.setStoreId(storeId);
        o.setStatus("ready");
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return orderRepository.save(o);
    }

    @Test
    void adminAssignExplicitRider_andUnassign() {
        String tenant = "default";
        RiderEntity r = mkRider(tenant);
        OrderEntity o = mkOrderReady(tenant, "store-1");
        OrderDeliveryEntity d = logisticsService.assignRiderAdmin(tenant, o.getId(), o.getStoreId(), r.getId());
        Assertions.assertNotNull(d);
        Assertions.assertEquals("RIDER_ASSIGNED", d.getStatus());
        Assertions.assertEquals(r.getId(), d.getRiderId());

        OrderDeliveryEntity unassigned = logisticsService.unassignRiderAdmin(tenant, d.getDeliveryId());
        Assertions.assertEquals("PENDING", unassigned.getStatus());
        Assertions.assertNull(unassigned.getRiderId());
        RiderEntity refreshed = riderRepository.findById(r.getId()).orElseThrow();
        Assertions.assertEquals("ONLINE", refreshed.getStatus());
    }

    @Test
    void adminAssign_crossTenantRider_shouldFail() {
        String tenant = "default";
        RiderEntity rOther = mkRider("other-tenant");
        OrderEntity o = mkOrderReady(tenant, "store-2");
        Assertions.assertThrows(com.bharatshop.error.ApiException.class, () ->
                logisticsService.assignRiderAdmin(tenant, o.getId(), o.getStoreId(), rOther.getId()));
    }
}