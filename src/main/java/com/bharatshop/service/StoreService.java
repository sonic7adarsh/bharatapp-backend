package com.bharatshop.service;

import com.bharatshop.domain.Store;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.entity.StoreZoneEntity;
import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
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
        // Explicit behavior: NO location = NO stores
        if (lat == null || lng == null) {
            log.warn("[GEO_STRICT] No location provided. Returning empty list.");
            return Collections.emptyList();
        }

        double radius = 10.0; // 10km radius as per MVP-1 requirement

        // Fetch stores within radius using DB calculation
        List<StoreEntity> stores = storeRepository.findNearbyStores(lat, lng, radius);

        log.info("[GEO_STRICT] Stores found in {}km radius: {}", radius, stores.size());

        // Filter and map
        List<Store> result = stores.stream()
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
        List<StoreEntity> list = storeRepository.findAll();
        // HARD PROOF LOGS (repository result)
        for (StoreEntity e : list) {
            log.error("[PROOF][STORE_REPO] store.id={} source=DB", e != null ? e.getId() : null);
        }
        return list.stream()
                .filter(s -> search == null || s.getName().toLowerCase().contains(search.toLowerCase()))
                .filter(s -> category == null || category.equalsIgnoreCase(s.getCategory()))
                .map(this::toDto)
                .sorted(Comparator.comparing(Store::getName))
                .collect(Collectors.toList());
    }

    public Store get(String id) {
        return storeRepository.findById(id).map(this::toDto).orElse(null);
    }

    public Optional<Store> getStoreById(String id) {
        return storeRepository.findById(id).map(this::toDto);
    }

    public Store add(Store s) {
        StoreEntity e = new StoreEntity();
        String id = s.getId();
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
        }
        e.setId(id); e.setName(s.getName()); e.setArea(s.getArea()); e.setCategory(s.getCategory());
        e.setAddress(s.getAddress());
        e.setLatitude(s.getLatitude());
        e.setLongitude(s.getLongitude());
        // ownership
        e.setOwnerId(s.getOwnerId());
        e.setOwnerPhone(s.getOwnerPhone());
        // default operational fields
        e.setStatus(s.getStatus() != null ? s.getStatus() : "open");
        e.setOrderingDisabled(Boolean.TRUE.equals(s.getOrderingDisabled()) ? true : false);
        e.setClosedReason(s.getClosedReason());
        e.setClosedUntil(s.getClosedUntil());
        e.setLogo(s.getLogo());
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
        s.setAddress(e.getAddress());
        s.setLatitude(e.getLatitude());
        s.setLongitude(e.getLongitude());
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
        return s;
    }
}