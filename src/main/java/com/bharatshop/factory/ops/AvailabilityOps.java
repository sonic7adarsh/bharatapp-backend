package com.bharatshop.factory.ops;

import com.bharatshop.domain.Store;
import java.util.Map;

/**
 * Operations for checking store availability and serviceability
 */
public interface AvailabilityOps {
    
    /**
     * Check if a store is available for ordering
     */
    Map<String, Object> checkStoreAvailability(String storeId, Double lat, Double lng);
    
    /**
     * Check inventory availability for products
     */
    Map<String, Object> checkInventoryAvailability(String storeId, String productId, int quantity);
    
    /**
     * Check if delivery is available to the given coordinates
     */
    Map<String, Object> checkDeliveryAvailability(String storeId, Double lat, Double lng);
}