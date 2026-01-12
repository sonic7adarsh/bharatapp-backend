package com.bharatshop.web;

import com.bharatshop.entity.RiderEntity;
import com.bharatshop.entity.RiderLocationEntity;
import com.bharatshop.repository.RiderLocationRepository;
import com.bharatshop.repository.RiderRepository;
import com.bharatshop.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/riders")
public class RiderController {
    private final RiderRepository riderRepository;
    private final RiderLocationRepository riderLocationRepository;

    public RiderController(RiderRepository riderRepository, RiderLocationRepository riderLocationRepository) {
        this.riderRepository = riderRepository;
        this.riderLocationRepository = riderLocationRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        String tenant = TenantContext.getTenant();
        String phone = req.get("phone");
        String riderId = req.get("riderId");
        RiderEntity rider = null;
        if (riderId != null) {
            rider = riderRepository.findById(riderId).orElse(null);
        } else if (phone != null) {
            rider = riderRepository.findByTenantIdAndPhone(tenant, phone).orElse(null);
        }
        if (rider == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid rider credentials"));
        }
        rider.setStatus("ONLINE");
        riderRepository.save(rider);
        return ResponseEntity.ok(Map.of("status","ok","riderId", rider.getId()));
    }

    @PostMapping("/status")
    public ResponseEntity<?> status(@RequestBody Map<String, String> req) {
        String riderId = req.get("riderId");
        String status = req.get("status");
        RiderEntity rider = riderRepository.findById(riderId).orElse(null);
        if (rider == null || status == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid riderId or status"));
        }
        rider.setStatus(status);
        riderRepository.save(rider);
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/location")
    public ResponseEntity<?> location(@RequestBody Map<String, Object> req) {
        String tenant = TenantContext.getTenant();
        String riderId = (String) req.get("riderId");
        Number lat = (Number) req.get("lat");
        Number lng = (Number) req.get("lng");
        if (riderId == null || lat == null || lng == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","riderId, lat, lng required"));
        }
        RiderLocationEntity loc = new RiderLocationEntity();
        loc.setId(UUID.randomUUID().toString());
        loc.setTenantId(tenant);
        loc.setRiderId(riderId);
        loc.setLat(lat.doubleValue());
        loc.setLng(lng.doubleValue());
        loc.setUpdatedAt(Instant.now());
        riderLocationRepository.save(loc);
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}