package com.bharatshop.web;

import com.bharatshop.entity.StoreZoneEntity;
import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
import com.bharatshop.service.GeoService;
import org.springframework.http.ResponseEntity;
import com.bharatshop.security.rbac.CustomerOnly;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storefront/serviceability")
@CustomerOnly
public class ServiceabilityController {
    private static final Logger log = LoggerFactory.getLogger(ServiceabilityController.class);
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
                                   @RequestParam double lng) {
        UserPrincipal up = UserPrincipal.current();
        String t = TenantContext.getTenant();
        log.info("Customer storefront: serviceability tenant={} userId={} storeId={} lat={} lng={} ", t, up != null ? up.getUserId() : null, storeId, lat, lng);
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