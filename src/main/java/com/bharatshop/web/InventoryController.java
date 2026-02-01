package com.bharatshop.web;

import com.bharatshop.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;
    public InventoryController(InventoryService inventoryService) { this.inventoryService = inventoryService; }

    private boolean ensureAdmin() {
        com.bharatshop.security.UserPrincipal up = com.bharatshop.security.UserPrincipal.current();
        return up != null && up.hasRole("ADMIN");
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody Map<String, Object> req) {
        if (!ensureAdmin()) return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        String productId = (String) req.get("productId");
        int quantity = ((Number) req.getOrDefault("quantity", 0)).intValue();
        if (productId == null || quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","productId and positive quantity required"));
        }
        boolean ok = inventoryService.reserve(tenantId, productId, quantity);
        return ResponseEntity.ok(Map.of("status", ok ? "ok" : "insufficient"));
    }

    @PostMapping("/release")
    public ResponseEntity<?> release(@RequestBody Map<String, Object> req) {
        if (!ensureAdmin()) return ResponseEntity.status(401).body(Map.of("status","error","message","Unauthorized"));
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        String productId = (String) req.get("productId");
        int quantity = ((Number) req.getOrDefault("quantity", 0)).intValue();
        if (productId == null || quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","productId and positive quantity required"));
        }
        boolean ok = inventoryService.release(tenantId, productId, quantity);
        return ResponseEntity.ok(Map.of("status", ok ? "ok" : "error"));
    }
}