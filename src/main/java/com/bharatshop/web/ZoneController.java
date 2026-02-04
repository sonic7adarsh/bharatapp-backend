package com.bharatshop.web;

import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.ZoneRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {
    private final ZoneRepository zoneRepository;
    private final com.bharatshop.repository.StoreZoneRepository storeZoneRepository;
    private final com.bharatshop.repository.RiderZoneRepository riderZoneRepository;
    public ZoneController(ZoneRepository zoneRepository,
                          com.bharatshop.repository.StoreZoneRepository storeZoneRepository,
                          com.bharatshop.repository.RiderZoneRepository riderZoneRepository) {
        this.zoneRepository = zoneRepository;
        this.storeZoneRepository = storeZoneRepository;
        this.riderZoneRepository = riderZoneRepository;
    }

    private boolean ensureAdmin() {
        com.bharatshop.security.UserPrincipal up = com.bharatshop.security.UserPrincipal.current();
        if (up == null || up.getRole() == null) return false;
        return "admin".equalsIgnoreCase(up.getRole());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> req) {
        if (!ensureAdmin()) return ResponseEntity.status(403).body(Map.of("status","error","message","Access denied"));
        ZoneEntity z = new ZoneEntity();
        z.setId(UUID.randomUUID().toString());
        z.setName((String) req.get("name"));
        z.setType((String) req.getOrDefault("type", "radius"));
        if ("radius".equalsIgnoreCase(z.getType())) {
            z.setCenterLat(((Number) req.getOrDefault("centerLat", 0)).doubleValue());
            z.setCenterLng(((Number) req.getOrDefault("centerLng", 0)).doubleValue());
            z.setRadiusMeters(((Number) req.getOrDefault("radiusMeters", 0)).intValue());
        } else {
            z.setPolygonJson((String) req.get("polygonJson"));
        }
        return ResponseEntity.ok(zoneRepository.save(z));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        if (!ensureAdmin()) return ResponseEntity.status(403).body(Map.of("status","error","message","Access denied"));
        return ResponseEntity.ok(zoneRepository.findAll());
    }

    @PostMapping("/attach-store")
    public ResponseEntity<?> attachStore(@RequestBody Map<String, String> req) {
        if (!ensureAdmin()) return ResponseEntity.status(403).body(Map.of("status","error","message","Access denied"));
        String storeId = req.get("storeId");
        String zoneId = req.get("zoneId");
        if (storeId == null || zoneId == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","storeId and zoneId required"));
        }
        com.bharatshop.entity.StoreZoneEntity link = new com.bharatshop.entity.StoreZoneEntity();
        link.setId(java.util.UUID.randomUUID().toString());
        link.setStoreId(storeId);
        link.setZoneId(zoneId);
        storeZoneRepository.save(link);
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/attach-rider")
    public ResponseEntity<?> attachRider(@RequestBody Map<String, String> req) {
        if (!ensureAdmin()) return ResponseEntity.status(403).body(Map.of("status","error","message","Access denied"));
        String riderId = req.get("riderId");
        String zoneId = req.get("zoneId");
        if (riderId == null || zoneId == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","riderId and zoneId required"));
        }
        com.bharatshop.entity.RiderZoneEntity link = new com.bharatshop.entity.RiderZoneEntity();
        link.setId(java.util.UUID.randomUUID().toString());
        link.setRiderId(riderId);
        link.setZoneId(zoneId);
        riderZoneRepository.save(link);
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}