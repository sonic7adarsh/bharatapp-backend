package com.bharatshop.service;

import com.bharatshop.domain.Store;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.repository.StoreRepository;
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

    public StoreService(StoreRepository storeRepository) { this.storeRepository = storeRepository; }

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
        return s;
    }
}