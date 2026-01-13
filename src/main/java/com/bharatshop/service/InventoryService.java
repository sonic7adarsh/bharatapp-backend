package com.bharatshop.service;

import com.bharatshop.entity.InventoryEntity;
import com.bharatshop.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    public InventoryService(InventoryRepository inventoryRepository) { this.inventoryRepository = inventoryRepository; }

    // In development, allow checkout to proceed without pre-seeded inventory
    @Value("${inventory.devFallback:false}")
    private boolean devFallback;

    @Transactional
    public boolean reserve(String tenantId, String productId, int quantity) {
        if (quantity <= 0) return false;
        InventoryEntity inv = inventoryRepository.lockByTenantAndProduct(tenantId, productId)
                .orElseGet(() -> {
                    InventoryEntity e = new InventoryEntity();
                    e.setId(java.util.UUID.randomUUID().toString());
                    e.setTenantId(tenantId);
                    e.setProductId(productId);
                    // Dev fallback: if enabled, seed available to requested quantity so reserve succeeds
                    e.setAvailable(devFallback ? quantity : 0);
                    e.setReserved(0);
                    e.setUpdatedAt(Instant.now());
                    return inventoryRepository.save(e);
                });
        int available = inv.getAvailable() == null ? 0 : inv.getAvailable();
        int reserved = inv.getReserved() == null ? 0 : inv.getReserved();
        if (available < quantity) {
            return false;
        }
        inv.setAvailable(available - quantity);
        inv.setReserved(reserved + quantity);
        inv.setUpdatedAt(Instant.now());
        inventoryRepository.save(inv);
        return true;
    }

    @Transactional
    public boolean release(String tenantId, String productId, int quantity) {
        if (quantity <= 0) return false;
        InventoryEntity inv = inventoryRepository.lockByTenantAndProduct(tenantId, productId).orElse(null);
        if (inv == null) return false;
        int available = inv.getAvailable() == null ? 0 : inv.getAvailable();
        int reserved = inv.getReserved() == null ? 0 : inv.getReserved();
        if (reserved < quantity) return false;
        inv.setReserved(reserved - quantity);
        inv.setAvailable(available + quantity);
        inv.setUpdatedAt(Instant.now());
        inventoryRepository.save(inv);
        return true;
    }

    /**
     * Consume reserved inventory after successful delivery.
     * This decrements the reserved count without increasing available.
     */
    @Transactional
    public boolean consume(String tenantId, String productId, int quantity) {
        if (quantity <= 0) return false;
        InventoryEntity inv = inventoryRepository.lockByTenantAndProduct(tenantId, productId).orElse(null);
        if (inv == null) return false;
        int reserved = inv.getReserved() == null ? 0 : inv.getReserved();
        if (reserved < quantity) return false;
        inv.setReserved(reserved - quantity);
        inv.setUpdatedAt(Instant.now());
        inventoryRepository.save(inv);
        return true;
    }

    /**
     * Check if inventory can be reserved without actually reserving it
     */
    public boolean canReserve(String tenantId, String productId, int quantity) {
        if (quantity <= 0) return false;
        
        // Use a non-locking query to check availability
        return inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
            .map(inv -> {
                int available = inv.getAvailable() == null ? 0 : inv.getAvailable();
                return available >= quantity;
            })
            // Dev fallback: if no inventory record exists, allow reservation check to pass in dev
            .orElse(devFallback);
    }

    /**
     * Get available inventory quantity for a product
     */
    public int getAvailable(String tenantId, String productId) {
        return inventoryRepository.findByTenantIdAndProductId(tenantId, productId)
            .map(inv -> inv.getAvailable() == null ? 0 : inv.getAvailable())
            .orElse(0);
    }

    /**
     * Placeholder hook for order acceptance to integrate inventory logic later.
     * Currently a no-op to satisfy MVP wiring without redesigning inventory.
     */
    public void reserveForOrder(String tenantId, String orderId) {
        // Intentionally left blank (MVP placeholder)
    }
}