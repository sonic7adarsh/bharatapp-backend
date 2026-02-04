package com.bharatshop.service;

import com.bharatshop.entity.CategoryEntity;
import com.bharatshop.entity.ProductEntity;
import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.CategoryRepository;
import com.bharatshop.repository.ProductRepository;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductDiscoveryService {

    private final GeoService geoService;
    private final ZoneRepository zoneRepository;
    private final StoreZoneRepository storeZoneRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductDiscoveryService(GeoService geoService,
                                   ZoneRepository zoneRepository,
                                   StoreZoneRepository storeZoneRepository,
                                   ProductRepository productRepository,
                                   CategoryRepository categoryRepository) {
        this.geoService = geoService;
        this.zoneRepository = zoneRepository;
        this.storeZoneRepository = storeZoneRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    private Set<String> resolveNearbyStoreIds(double lat, double lng) {
        // MVP: Fetch all zones and filter in memory. Optimized approach would use spatial DB index.
        List<ZoneEntity> allZones = zoneRepository.findAll(); 
        Set<String> nearbyStoreIds = new HashSet<>();

        for (ZoneEntity zone : allZones) {
            if (geoService.isPointInZone(lat, lng, zone)) {
                storeZoneRepository.findByZoneId(zone.getId())
                        .forEach(sz -> nearbyStoreIds.add(sz.getStoreId()));
            }
        }
        return nearbyStoreIds;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductsByCategory(String categoryId, double lat, double lng) {
        Set<String> storeIds = resolveNearbyStoreIds(lat, lng);
        if (storeIds.isEmpty()) return Collections.emptyList();

        List<ProductEntity> products = productRepository.findByCategoryIdAndStoreIdInAndActiveTrue(categoryId, storeIds);

        // Deduplicate by name (basic DISTINCT)
        // We pick the first one encountered (arbitrary store)
        Map<String, ProductEntity> distinctProducts = new LinkedHashMap<>();
        for (ProductEntity p : products) {
            distinctProducts.putIfAbsent(p.getName(), p);
        }

        return distinctProducts.values().stream()
                .map(this::mapProduct)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(String query, double lat, double lng) {
        Set<String> storeIds = resolveNearbyStoreIds(lat, lng);
        
        // 1. Search Products
        List<ProductEntity> products = storeIds.isEmpty() ? Collections.emptyList() 
                : productRepository.findByNameContainingIgnoreCaseAndStoreIdInAndActiveTrue(query, storeIds);
        
        Map<String, ProductEntity> uniqueProducts = new LinkedHashMap<>();
        for (ProductEntity p : products) {
            uniqueProducts.putIfAbsent(p.getName(), p);
        }

        // 2. Search Categories
        List<CategoryEntity> categories = categoryRepository.findByNameContainingIgnoreCase(query);

        return Map.of(
            "products", uniqueProducts.values().stream().map(this::mapProduct).collect(Collectors.toList()),
            "categories", categories
        );
    }

    private Map<String, Object> mapProduct(ProductEntity p) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", p.getId());
        map.put("name", p.getName());
        map.put("price", p.getPrice());
        map.put("storeId", p.getStoreId());
        map.put("image", p.getImage());
        return map;
    }
}
