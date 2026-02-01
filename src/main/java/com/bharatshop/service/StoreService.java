package com.bharatshop.service;

import com.bharatshop.domain.Store;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.entity.StoreZoneEntity;
import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
import com.bharatshop.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoreService {
    private static final Logger log = LoggerFactory.getLogger(StoreService.class);
    private final StoreRepository storeRepository;
    private final ZoneRepository zoneRepository;
    private final StoreZoneRepository storeZoneRepository;
    private final GeoService geoService;

    public StoreService(StoreRepository storeRepository,
                        ZoneRepository zoneRepository,
                        StoreZoneRepository storeZoneRepository,
                        GeoService geoService) {
        this.storeRepository = storeRepository;
        this.zoneRepository = zoneRepository;
        this.storeZoneRepository = storeZoneRepository;
        this.geoService = geoService;
    }

    public List<Store> listNearby(Double lat, Double lng, String search, String category) {
        String tenant = TenantContext.getTenant();
        if (tenant == null) { throw new IllegalStateException("TenantContext missing"); }
        
        // Explicit behavior: NO location = NO stores
        if (lat == null || lng == null) {
            log.warn("[GEO_STRICT] No location provided. Returning empty list.");
            return Collections.emptyList();
        }

        // 1. Get candidate zones (Radius filtered by DB, Polygon fetched for memory check)
        List<ZoneEntity> candidateZones = zoneRepository.findNearbyZones(tenant, lat, lng);
        
        // 2. Filter zones (Double check Radius for precision, Strict check for Polygon)
        Set<String> validZoneIds = candidateZones.stream()
            .filter(z -> geoService.isPointInZone(lat, lng, z))
            .map(ZoneEntity::getId)
            .collect(Collectors.toSet());
            
        log.info("[GEO_STRICT] Zones matched: {} (lat={}, lng={})", validZoneIds.size(), lat, lng);

        if (validZoneIds.isEmpty()) {
            // FALLBACK: If no zones found, use simple distance search (MVP/Profiling)
            log.warn("[GEO_FALLBACK] No zones found. Falling back to simple distance radius (50km).");
            List<StoreEntity> allStores = storeRepository.findByTenantId(tenant);
            List<Store> result = allStores.stream()
                .filter(s -> s.getLat() != null && s.getLng() != null)
                .filter(s -> geoService.haversine(lat, lng, s.getLat(), s.getLng()) <= 50.0) // 50km radius
                .filter(s -> "open".equalsIgnoreCase(s.getStatus()))
                .filter(s -> !Boolean.TRUE.equals(s.getOrderingDisabled()))
                .filter(s -> search == null || (s.getName() != null && s.getName().toLowerCase().contains(search.toLowerCase())))
                .filter(s -> category == null || (s.getCategory() != null && s.getCategory().equalsIgnoreCase(category)))
                .map(this::toDto)
                .sorted(Comparator.comparing(Store::getName))
                .collect(Collectors.toList());
            
            log.info("[GEO_FALLBACK] Stores returned: {}", result.size());
            return result;
        }
        
        // 3. Get store IDs in these zones
        List<StoreZoneEntity> storeZones = storeZoneRepository.findByTenantIdAndZoneIdIn(tenant, validZoneIds);
        Set<String> storeIds = storeZones.stream()
            .map(StoreZoneEntity::getStoreId)
            .collect(Collectors.toSet());
            
        log.info("[GEO_STRICT] Store mappings found: {}", storeIds.size());

        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 4. Fetch stores
        List<StoreEntity> stores = storeRepository.findAllById(storeIds);
        
        // 5. Filter and map
        List<Store> result = stores.stream()
            .filter(s -> tenant.equals(s.getTenantId())) // Safety check
            .filter(s -> "open".equalsIgnoreCase(s.getStatus())) // Strict status check
            .filter(s -> !Boolean.TRUE.equals(s.getOrderingDisabled())) // Strict ordering check
            .filter(s -> search == null || s.getName().toLowerCase().contains(search.toLowerCase()))
            .filter(s -> category == null || category.equalsIgnoreCase(s.getCategory()))
            .map(this::toDto)
            .sorted(Comparator.comparing(Store::getName))
            .collect(Collectors.toList());

        log.info("[GEO_STRICT] Stores returned: {}", result.size());
        return result;
    }

    public List<Store> list(String search, String category) {
        String tenant = TenantContext.getTenant();
        if (tenant == null) { throw new IllegalStateException("TenantContext missing"); }
        List<StoreEntity> list = storeRepository.findByTenantId(tenant);
        // HARD PROOF LOGS (repository result)
        for (StoreEntity e : list) {
            log.error("[PROOF][STORE_REPO] store.id={} source=DB", e != null ? e.getId() : null);
        }
        return list.stream().map(this::toDto).sorted(Comparator.comparing(Store::getName)).collect(Collectors.toList());
    }

    public Store get(String id) {
        String tenant = TenantContext.getTenant();
        if (tenant == null) { throw new IllegalStateException("TenantContext missing"); }
        return storeRepository.findByIdAndTenantId(id, tenant).map(this::toDto).orElse(null);
    }

    public Optional<Store> getStoreById(String id, String tenantId) {
        return storeRepository.findByIdAndTenantId(id, tenantId).map(this::toDto);
    }

    public Store add(Store s) {
        StoreEntity e = new StoreEntity();
        String id = s.getId();
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
        }
        e.setId(id); e.setName(s.getName()); e.setArea(s.getArea()); e.setCategory(s.getCategory());
        // ownership
        e.setOwnerId(s.getOwnerId());
        e.setOwnerPhone(s.getOwnerPhone());
        
        // Address fields
        e.setFullAddress(s.getFullAddress());
        e.setShopNo(s.getShopNo());
        e.setBuilding(s.getBuilding());
        e.setStreet(s.getStreet());
        e.setCity(s.getCity());
        e.setPincode(s.getPincode());
        e.setLat(s.getLat());
        e.setLng(s.getLng());
        
        // default operational fields
        e.setStatus(s.getStatus() != null ? s.getStatus() : "open");
        e.setOrderingDisabled(Boolean.TRUE.equals(s.getOrderingDisabled()) ? true : false);
        e.setClosedReason(s.getClosedReason());
        e.setClosedUntil(s.getClosedUntil());
        e.setLogo(s.getLogo());
        // Set tenant
        String tenant = TenantContext.getTenant();
        if (tenant != null && !tenant.isBlank()) { e.setTenantId(tenant); }
        e.setUpdatedAt(java.time.Instant.now());
        e = storeRepository.save(e);
        if (e.getId() == null) {
            throw new IllegalStateException("Returning non-persisted entity");
        }
        return toDto(e);
    }

    private Store toDto(StoreEntity e) {
        if (e == null || e.getId() == null) {
            throw new IllegalStateException("Corrupt entity loaded from DB: id is null");
        }
        Store s = new Store(e.getId(), e.getName(), e.getArea(), e.getCategory());
        // HARD PROOF LOGS (entity to domain mapping)
        log.error("[PROOF][STORE_MAP] entity.id={} mapped.id={}", e != null ? e.getId() : null, s.getId());
        s.setOwnerId(e.getOwnerId());
        s.setOwnerPhone(e.getOwnerPhone());
        s.setStatus(e.getStatus());
        s.setOrderingDisabled(e.getOrderingDisabled());
        s.setClosedReason(e.getClosedReason());
        s.setClosedUntil(e.getClosedUntil());
        s.setLogo(e.getLogo());
        s.setUpdatedAt(e.getUpdatedAt());
        
        // Address fields
        s.setFullAddress(e.getFullAddress());
        s.setShopNo(e.getShopNo());
        s.setBuilding(e.getBuilding());
        s.setStreet(e.getStreet());
        s.setCity(e.getCity());
        s.setPincode(e.getPincode());
        s.setLat(e.getLat());
        s.setLng(e.getLng());
        
        return s;
    }
}