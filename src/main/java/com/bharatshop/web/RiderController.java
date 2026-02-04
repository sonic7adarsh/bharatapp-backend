package com.bharatshop.web;

import com.bharatshop.entity.RiderEntity;
import com.bharatshop.entity.RiderLocationEntity;
import com.bharatshop.repository.RiderLocationRepository;
import com.bharatshop.repository.RiderRepository;
import com.bharatshop.security.JwtService;
import com.bharatshop.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController("webRiderController")
@RequestMapping("/api/riders")
public class RiderController {
    private final RiderRepository riderRepository;
    private final RiderLocationRepository riderLocationRepository;
    private final JwtService jwtService;

    public RiderController(RiderRepository riderRepository, RiderLocationRepository riderLocationRepository, JwtService jwtService) {
        this.riderRepository = riderRepository;
        this.riderLocationRepository = riderLocationRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        // tenant is ignored
        String phone = req.get("phone");
        String riderId = req.get("riderId");
        RiderEntity rider = null;
        if (riderId != null) {
            rider = riderRepository.findById(riderId).orElse(null);
        } else if (phone != null) {
            rider = riderRepository.findByPhone(phone).orElse(null);
        }
        if (rider == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid rider credentials"));
        }
        rider.setStatus("ONLINE");
        riderRepository.save(rider);
        // Issue JWT for rider with role=RIDER and activeRole=RIDER, include roles array
        String token = jwtService.generateTokenWithRoles(
                rider.getId(),
                rider.getName() == null ? "Rider" : rider.getName(),
                "RIDER",
                "RIDER",
                java.util.List.of("RIDER")
        );
        return ResponseEntity.ok(Map.of(
                "status","ok",
                "riderId", rider.getId(),
                "token", token
        ));
    }

    @PostMapping("/status")
    public ResponseEntity<?> status(@RequestBody Map<String, String> req) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null || up.getRole() == null || !"RIDER".equalsIgnoreCase(up.getRole())) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String riderId = up.getUserId();
        String status = req.get("status");
        RiderEntity rider = riderRepository.findById(riderId).orElse(null);
        if (rider == null || status == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","Invalid status"));
        }
        rider.setStatus(status);
        riderRepository.save(rider);
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/location")
    public ResponseEntity<?> location(@RequestBody Map<String, Object> req) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null || up.getRole() == null || !"RIDER".equalsIgnoreCase(up.getRole())) {
            return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        }
        String riderId = up.getUserId();
        Number lat = (Number) req.get("lat");
        Number lng = (Number) req.get("lng");
        if (lat == null || lng == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","lat, lng required"));
        }
        RiderLocationEntity loc = new RiderLocationEntity();
        loc.setId(UUID.randomUUID().toString());
        loc.setRiderId(riderId);
        loc.setLat(lat.doubleValue());
        loc.setLng(lng.doubleValue());
        loc.setUpdatedAt(Instant.now());
        riderLocationRepository.save(loc);
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}