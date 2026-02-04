package com.bharatshop.policy;

import com.bharatshop.domain.Store;
import com.bharatshop.domain.CartItem;
import com.bharatshop.error.ErrorCode;
import com.bharatshop.service.InventoryService;
import com.bharatshop.service.StoreService;
import com.bharatshop.service.GeoService;
import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.entity.StoreZoneEntity;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StoreAvailabilityPolicy {
    
    private final InventoryService inventoryService;
    private final StoreService storeService;
    private final GeoService geoService;
    private final StoreZoneRepository storeZoneRepository;
    private final ZoneRepository zoneRepository;
    @Value("${serviceability.devFallback:true}")
    private boolean devFallback;
    
    @Value("${inventory.devFallback:false}")
    private boolean inventoryDevFallback;
    
    public StoreAvailabilityPolicy(InventoryService inventoryService, 
                                 StoreService storeService, 
                                 GeoService geoService,
                                 StoreZoneRepository storeZoneRepository,
                                 ZoneRepository zoneRepository) {
        this.inventoryService = inventoryService;
        this.storeService = storeService;
        this.geoService = geoService;
        this.storeZoneRepository = storeZoneRepository;
        this.zoneRepository = zoneRepository;
    }
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Basic store availability check (existing functionality)
     */
    public Map<String, Object> availabilityError(Store store) {
        if (store == null) {
            return Map.of(
                "code", ErrorCode.STORE_NOT_FOUND.name(),
                "message", ErrorCode.STORE_NOT_FOUND.getDefaultMessage()
            );
        }
        
        boolean disabled = Boolean.TRUE.equals(store.getOrderingDisabled());
        boolean closed = store.getStatus() != null && store.getStatus().equalsIgnoreCase("closed");
        boolean untilClosed = store.getClosedUntil() != null && Instant.now().isBefore(store.getClosedUntil());

        if (disabled) {
            return Map.of(
                "code", ErrorCode.STORE_ORDERING_DISABLED.name(),
                "message", ErrorCode.STORE_ORDERING_DISABLED.getDefaultMessage(),
                "storeId", store.getId()
            );
        }
        
        if (closed) {
            return Map.of(
                "code", ErrorCode.STORE_CLOSED.name(),
                "message", ErrorCode.STORE_CLOSED.getDefaultMessage(),
                "storeId", store.getId()
            );
        }
        
        if (untilClosed) {
            String timeStr = TIME_FORMATTER.format(store.getClosedUntil().atZone(java.time.ZoneOffset.UTC));
            return Map.of(
                "code", ErrorCode.STORE_TEMPORARILY_CLOSED.name(),
                "message", "Store is temporarily closed until " + timeStr,
                "storeId", store.getId(),
                "closedUntil", store.getClosedUntil()
            );
        }
        
        return null;
    }
    
    /**
     * Check store availability with zone serviceability
     */
    public Map<String, Object> availabilityError(Store store, Double deliveryLat, Double deliveryLng) {
        // Basic store availability check
        Map<String, Object> basicError = availabilityError(store);
        if (basicError != null) {
            return basicError;
        }
        
        // Zone serviceability check
        if (deliveryLat != null && deliveryLng != null) {
            Map<String, Object> zoneError = checkZoneServiceability(store, deliveryLat, deliveryLng);
            if (zoneError != null) {
                return zoneError;
            }
        }
        
        return null; // Store is available
    }

    /**
     * Comprehensive availability check including inventory and zone serviceability
     */
    public Map<String, Object> availabilityError(Store store, List<CartItem> items, Double deliveryLat, Double deliveryLng) {
        // Step 1: Check basic store availability
        Map<String, Object> basicError = availabilityError(store);
        if (basicError != null) {
            return basicError;
        }
        
        // Step 2: Check inventory availability
        if (items != null && !items.isEmpty() && !inventoryDevFallback) {
            Map<String, Object> inventoryError = checkInventoryAvailability(items);
            if (inventoryError != null) {
                return inventoryError;
            }
        }
        
        // Step 3: Check zone serviceability
        if (deliveryLat != null && deliveryLng != null) {
            Map<String, Object> zoneError = checkZoneServiceability(store, deliveryLat, deliveryLng);
            if (zoneError != null) {
                return zoneError;
            }
        }
        
        return null;
    }
    
    private Map<String, Object> checkInventoryAvailability(List<CartItem> items) {
        // In development, skip inventory checks entirely when dev fallback is enabled
        if (inventoryDevFallback) {
            return null;
        }
        
        // Group items by product ID to check total quantity needed
        Map<String, Integer> productQuantities = items.stream()
            .collect(Collectors.groupingBy(
                CartItem::getId,
                Collectors.summingInt(CartItem::getQuantity)
            ));
        
        // Check if any product has insufficient inventory
        List<String> unavailableProducts = productQuantities.entrySet().stream()
            .filter(entry -> !inventoryService.canReserve(entry.getKey(), entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (!unavailableProducts.isEmpty()) {
            return Map.of(
                "code", ErrorCode.INSUFFICIENT_INVENTORY.name(),
                "message", ErrorCode.INSUFFICIENT_INVENTORY.getDefaultMessage(),
                "unavailableProducts", unavailableProducts,
                "suggestion", "Please update your cart and try again"
            );
        }
        
        return null;
    }
    
    public Map<String, Object> checkZoneServiceability(Store store, Double deliveryLat, Double deliveryLng) {
        if (store == null) {
            return Map.of(
                "code", ErrorCode.STORE_NOT_FOUND.name(),
                "message", ErrorCode.STORE_NOT_FOUND.getDefaultMessage()
            );
        }

        // In development, optionally bypass zone serviceability checks
        if (devFallback) {
            return null;
        }

        // Get zones served by this store
        List<StoreZoneEntity> storeZones = storeZoneRepository.findByStoreId(store.getId());
        
        if (storeZones.isEmpty()) {
            return Map.of(
                "code", ErrorCode.DELIVERY_NOT_AVAILABLE.name(), 
                "message", ErrorCode.DELIVERY_NOT_AVAILABLE.getDefaultMessage(),
                "storeId", store.getId()
            );
        }
        
        // Check if delivery location is in any of the store's zones using actual zone geometry
        boolean serviceable = storeZones.stream()
            .anyMatch(storeZone -> {
                ZoneEntity zone = zoneRepository.findById(storeZone.getZoneId()).orElse(null);
                return zone != null && geoService.isPointInZone(deliveryLat, deliveryLng, zone);
            });
        
        if (!serviceable) {
            return Map.of(
                "code", ErrorCode.STORE_OUT_OF_SERVICE_AREA.name(),
                "message", ErrorCode.STORE_OUT_OF_SERVICE_AREA.getDefaultMessage(),
                "storeId", store.getId(),
                "lat", deliveryLat,
                "lng", deliveryLng
            );
        }
        
        return null;
    }
}