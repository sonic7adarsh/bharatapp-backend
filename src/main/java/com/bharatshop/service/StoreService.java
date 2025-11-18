package com.bharatshop.service;

import com.bharatshop.domain.Store;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.repository.StoreRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoreService {
    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) { this.storeRepository = storeRepository; }

    public List<Store> list(String search, String category) {
        List<StoreEntity> list;
        if (category != null && !category.isBlank()) list = storeRepository.findByCategoryIgnoreCase(category);
        else if (search != null && !search.isBlank()) list = storeRepository.findByNameContainingIgnoreCase(search);
        else list = storeRepository.findAll();
        return list.stream().map(this::toDto).sorted(Comparator.comparing(Store::getName)).collect(Collectors.toList());
    }

    public Store get(String id) { return storeRepository.findById(id).map(this::toDto).orElse(null); }

    public Store add(Store s) {
        StoreEntity e = new StoreEntity();
        e.setId(s.getId()); e.setName(s.getName()); e.setArea(s.getArea()); e.setCategory(s.getCategory());
        // ownership
        e.setOwnerId(s.getOwnerId());
        e.setOwnerPhone(s.getOwnerPhone());
        // default operational fields
        e.setStatus(s.getStatus() != null ? s.getStatus() : "open");
        e.setOrderingDisabled(Boolean.TRUE.equals(s.getOrderingDisabled()) ? true : false);
        e.setClosedReason(s.getClosedReason());
        e.setClosedUntil(s.getClosedUntil());
        e.setUpdatedAt(java.time.Instant.now());
        e = storeRepository.save(e);
        return toDto(e);
    }

    private Store toDto(StoreEntity e) {
        Store s = new Store(e.getId(), e.getName(), e.getArea(), e.getCategory());
        s.setOwnerId(e.getOwnerId());
        s.setOwnerPhone(e.getOwnerPhone());
        s.setStatus(e.getStatus());
        s.setOrderingDisabled(e.getOrderingDisabled());
        s.setClosedReason(e.getClosedReason());
        s.setClosedUntil(e.getClosedUntil());
        s.setUpdatedAt(e.getUpdatedAt());
        return s;
    }
}