package com.bharatshop.service;

import com.bharatshop.entity.ZoneEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@Transactional
public class ZoneServiceTest {
    @Autowired ZoneService zoneService;

    @BeforeEach
    void setup() { 
        // TenantContext removed
    }

    @Test
    void createRadiusZone_valid() {
        Map<String, Object> req = new HashMap<>();
        req.put("name", "Zone A");
        req.put("type", "radius");
        req.put("centerLat", 12.9);
        req.put("centerLng", 77.5);
        req.put("radiusMeters", 200);
        ZoneEntity z = zoneService.createZone(req);
        Assertions.assertNotNull(z.getId());
        // Tenant check removed
    }

    @Test
    void createRadiusZone_invalidRadius() {
        Map<String, Object> req = new HashMap<>();
        req.put("name", "Zone B");
        req.put("type", "radius");
        req.put("centerLat", 12.9);
        req.put("centerLng", 77.5);
        req.put("radiusMeters", 5); // too small
        Assertions.assertThrows(com.bharatshop.error.ApiException.class, () -> zoneService.createZone(req));
    }

    @Test
    void createPolygonZone_invalidPolygon() {
        Map<String, Object> req = new HashMap<>();
        req.put("name", "Zone C");
        req.put("type", "polygon");
        req.put("polygonJson", "[]");
        Assertions.assertThrows(com.bharatshop.error.ApiException.class, () -> zoneService.createZone(req));
    }
}