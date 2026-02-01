package com.bharatshop.web.admin;

import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.service.ZoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/zones")
@PreAuthorize("hasRole('ADMIN')")
public class AdminZoneController {
    private final ZoneService zoneService;

    public AdminZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> req) {
        ZoneEntity z = zoneService.createZone(req);
        return ResponseEntity.ok(Map.of(
                "status", "created",
                "zoneId", z.getId()
        ));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(zoneService.getZones());
    }

    @PutMapping("/{zoneId}")
    public ResponseEntity<?> update(@PathVariable String zoneId, @RequestBody Map<String, Object> req) {
        ZoneEntity z = zoneService.updateZone(zoneId, req);
        return ResponseEntity.ok(Map.of(
                "status", "updated",
                "zoneId", z.getId()
        ));
    }

    @DeleteMapping("/{zoneId}")
    public ResponseEntity<?> delete(@PathVariable String zoneId) {
        zoneService.deleteZone(zoneId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}