package com.bharatshop.web;

import com.bharatshop.domain.Store;
import com.bharatshop.policy.StoreAvailabilityPolicy;
import com.bharatshop.service.StoreService;
import com.bharatshop.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final StoreAvailabilityPolicy availabilityPolicy;
    private final StoreService storeService;

    public AvailabilityController(StoreAvailabilityPolicy availabilityPolicy, StoreService storeService) {
        this.availabilityPolicy = availabilityPolicy;
        this.storeService = storeService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam(name = "storeId") String storeId,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng
    ) {
        String tenant = TenantContext.getTenant();
        if (tenant == null || tenant.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", "VALIDATION_ERROR");
            error.put("message", "Missing X-Tenant-Domain header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Store store = storeService.get(storeId);
        if (store == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", "STORE_NOT_FOUND",
                "message", "Store not found",
                "storeId", storeId
            ));
        }

        Map<String, Object> error = availabilityPolicy.availabilityError(store, lat, lng);
        if (error != null) {
            return ResponseEntity.ok(Map.of(
                "available", false,
                "reason", error
            ));
        }

        return ResponseEntity.ok(Map.of(
            "available", true,
            "message", "Store is available for ordering"
        ));
    }
}