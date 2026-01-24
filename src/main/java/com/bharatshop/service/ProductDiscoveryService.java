package com.bharatshop.service;

import com.bharatshop.entity.CategoryEntity;
import com.bharatshop.entity.ProductEntity;
import com.bharatshop.entity.StoreZoneEntity;
import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.CategoryRepository;
import com.bharatshop.repository.ProductRepository;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
import com.bharatshop.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductDiscoveryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ZoneRepository zoneRepository;
    private final StoreZoneRepository storeZoneRepository;
    private final GeoService geoService;

    public ProductDiscoveryService(CategoryRepository categoryRepository,
                                   ProductRepository productRepository,
                                   ZoneRepository zoneRepository,
                                   StoreZoneRepository storeZoneRepository,
                                   GeoService geoService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.zoneRepository = zoneRepository;
        this.storeZoneRepository = storeZoneRepository;
        this.geoService = geoService;
    }

    public List<CategoryEntity> getGlobalCategories() {
        return categoryRepository.findByIsGlobalTrueOrderByPriorityAsc();
    }

    public List<Map<String, Object>> getProductsByCategory(String categoryId, Double lat, Double lng) {
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
                .map(this::mapProductToDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> search(String query, Double lat, Double lng) {
        Set<String> storeIds = resolveNearbyStoreIds(lat, lng);
        
        // 1. Search Products
        List<ProductEntity> products = Collections.emptyList();
        if (!storeIds.isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCaseAndStoreIdInAndActiveTrue(query, storeIds);
        }

        // Deduplicate products
        Map<String, ProductEntity> distinctProducts = new LinkedHashMap<>();
        for (ProductEntity p : products) {
            distinctProducts.putIfAbsent(p.getName(), p);
        }
        List<Map<String, Object>> productDtos = distinctProducts.values().stream()
                .map(this::mapProductToDto)
                .collect(Collectors.toList());

        // 2. Search Categories
        List<CategoryEntity> categories = categoryRepository.findByNameContainingIgnoreCaseAndIsGlobalTrue(query);

        return Map.of(
            "products", productDtos,
            "categories", categories
        );
    }

    private Set<String> resolveNearbyStoreIds(Double lat, Double lng) {
        if (lat == null || lng == null) return Collections.emptySet();

        String tenantId = TenantContext.getTenant();
        if (tenantId == null) tenantId = "default"; // Fallback

        // Get all zones for tenant
        List<ZoneEntity> zones = zoneRepository.findByTenantId(tenantId);
        
        // Filter zones containing the point
        Set<String> zoneIds = zones.stream()
                .filter(z -> geoService.isPointInZone(lat, lng, z))
                .map(ZoneEntity::getId)
                .collect(Collectors.toSet());

        if (zoneIds.isEmpty()) return Collections.emptySet();

        // Get stores in these zones
        List<StoreZoneEntity> storeZones = storeZoneRepository.findByTenantIdAndZoneIdIn(tenantId, zoneIds);
        
        return storeZones.stream()
                .map(StoreZoneEntity::getStoreId)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> mapProductToDto(ProductEntity p) {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", p.getId());
        map.put("name", p.getName());
        map.put("price", p.getPrice());
        map.put("storeId", p.getStoreId());
        // Image is useful for discovery
        if (p.getImage() != null) map.put("image", p.getImage());
        return map;
    }
}
