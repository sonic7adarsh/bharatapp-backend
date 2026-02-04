package com.bharatshop.service;

import com.bharatshop.domain.Store;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.entity.StoreZoneEntity;
import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreDiscoveryTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private StoreZoneRepository storeZoneRepository;
    @Mock
    private GeoService geoService;

    @InjectMocks
    private StoreService storeService;

    @BeforeEach
    void setUp() {
        // Tenant setup removed
    }

    @AfterEach
    void tearDown() {
        // Tenant cleanup removed
    }

    @Test
    void listNearby_NoLocation_ReturnsEmpty() {
        List<Store> result = storeService.listNearby(null, null, null, null);
        assertTrue(result.isEmpty());
        verifyNoInteractions(zoneRepository);
    }

    @Test
    void listNearby_NoZones_ReturnsEmpty() {
        when(zoneRepository.findNearbyZones(anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        List<Store> result = storeService.listNearby(12.9716, 77.5946, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void listNearby_ValidZone_ValidStore_ReturnsStore() {
        ZoneEntity zone = new ZoneEntity();
        zone.setId("zone1");
        when(zoneRepository.findNearbyZones(anyDouble(), anyDouble()))
                .thenReturn(Collections.singletonList(zone));
        when(geoService.isPointInZone(anyDouble(), anyDouble(), any(ZoneEntity.class)))
                .thenReturn(true);

        StoreZoneEntity storeZone = new StoreZoneEntity();
        storeZone.setStoreId("store1");
        when(storeZoneRepository.findByZoneIdIn(any()))
                .thenReturn(Collections.singletonList(storeZone));

        StoreEntity store = new StoreEntity();
        store.setId("store1");
        store.setName("Test Store");
        // TenantId removed
        store.setStatus("open");
        store.setOrderingDisabled(false);
        when(storeRepository.findAllById(any())).thenReturn(Collections.singletonList(store));

        List<Store> result = storeService.listNearby(12.9716, 77.5946, null, null);
        assertEquals(1, result.size());
        assertEquals("Test Store", result.get(0).getName());
    }

    @Test
    void listNearby_ClosedStore_ReturnsEmpty() {
        ZoneEntity zone = new ZoneEntity();
        zone.setId("zone1");
        when(zoneRepository.findNearbyZones(anyDouble(), anyDouble()))
                .thenReturn(Collections.singletonList(zone));
        when(geoService.isPointInZone(anyDouble(), anyDouble(), any(ZoneEntity.class)))
                .thenReturn(true);

        StoreZoneEntity storeZone = new StoreZoneEntity();
        storeZone.setStoreId("store1");
        when(storeZoneRepository.findByZoneIdIn(any()))
                .thenReturn(Collections.singletonList(storeZone));

        StoreEntity store = new StoreEntity();
        store.setId("store1");
        store.setStatus("closed"); // Closed store
        // TenantId removed
        when(storeRepository.findAllById(any())).thenReturn(Collections.singletonList(store));

        List<Store> result = storeService.listNearby(12.9716, 77.5946, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void listNearby_OrderingDisabled_ReturnsEmpty() {
        ZoneEntity zone = new ZoneEntity();
        zone.setId("zone1");
        when(zoneRepository.findNearbyZones(anyDouble(), anyDouble()))
                .thenReturn(Collections.singletonList(zone));
        when(geoService.isPointInZone(anyDouble(), anyDouble(), any(ZoneEntity.class)))
                .thenReturn(true);

        StoreZoneEntity storeZone = new StoreZoneEntity();
        storeZone.setStoreId("store1");
        when(storeZoneRepository.findByZoneIdIn(any()))
                .thenReturn(Collections.singletonList(storeZone));

        StoreEntity store = new StoreEntity();
        store.setId("store1");
        store.setStatus("open");
        store.setOrderingDisabled(true); // Ordering disabled
        // TenantId removed
        when(storeRepository.findAllById(any())).thenReturn(Collections.singletonList(store));

        List<Store> result = storeService.listNearby(12.9716, 77.5946, null, null);
        assertTrue(result.isEmpty());
    }
}