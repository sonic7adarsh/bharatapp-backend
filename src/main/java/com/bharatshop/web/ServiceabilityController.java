package com.bharatshop.web;

import com.bharatshop.entity.StoreZoneEntity;
import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
import com.bharatshop.service.GeoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storefront/serviceability")
public class ServiceabilityController {
    private final StoreZoneRepository storeZoneRepository;
    private final ZoneRepository zoneRepository;
    private final GeoService geoService;

    public ServiceabilityController(StoreZoneRepository storeZoneRepository, ZoneRepository zoneRepository, GeoService geoService) {
        this.storeZoneRepository = storeZoneRepository;
        this.zoneRepository = zoneRepository;
        this.geoService = geoService;
    }

    @GetMapping
    public ResponseEntity<?> check(@RequestParam String storeId,
                                   @RequestParam double lat,
                                   @RequestParam double lng,
                                   @RequestParam(required = false) String tenantId) {
        String t = tenantId != null ? tenantId : com.bharatshop.tenant.TenantContext.getTenant();
        List<StoreZoneEntity> links = storeZoneRepository.findByTenantIdAndStoreId(t, storeId);
        for (StoreZoneEntity link : links) {
            ZoneEntity zone = zoneRepository.findById(link.getZoneId()).orElse(null);
            if (zone != null && geoService.isPointInZone(lat, lng, zone)) {
                return ResponseEntity.ok(Map.of("serviceable", true, "zoneId", zone.getId()));
            }
        }
        return ResponseEntity.ok(Map.of("serviceable", false));
    }
}