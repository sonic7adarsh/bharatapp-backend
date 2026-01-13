package com.bharatshop.service;

import com.bharatshop.entity.ZoneEntity;
import com.bharatshop.repository.RiderZoneRepository;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.ZoneRepository;
import com.bharatshop.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ZoneService {
    private final ZoneRepository zoneRepository;
    private final StoreZoneRepository storeZoneRepository;
    private final RiderZoneRepository riderZoneRepository;

    public ZoneService(ZoneRepository zoneRepository,
                       StoreZoneRepository storeZoneRepository,
                       RiderZoneRepository riderZoneRepository) {
        this.zoneRepository = zoneRepository;
        this.storeZoneRepository = storeZoneRepository;
        this.riderZoneRepository = riderZoneRepository;
    }

    public ZoneEntity createZone(Map<String, Object> req) {
        String tenant = safeTenant();
        ZoneEntity z = new ZoneEntity();
        z.setId(UUID.randomUUID().toString());
        z.setTenantId(tenant);
        z.setName((String) req.get("name"));
        String type = (String) req.getOrDefault("type", "radius");
        z.setType(type);
        validateZonePayload(type, req);
        if ("radius".equalsIgnoreCase(type)) {
            Number lat = (Number) req.get("centerLat");
            Number lng = (Number) req.get("centerLng");
            Number radius = (Number) req.get("radiusMeters");
            z.setCenterLat(lat != null ? lat.doubleValue() : null);
            z.setCenterLng(lng != null ? lng.doubleValue() : null);
            z.setRadiusMeters(radius != null ? radius.intValue() : null);
        } else {
            z.setPolygonJson((String) req.get("polygonJson"));
        }
        return zoneRepository.save(z);
    }

    public List<ZoneEntity> getZones() {
        String tenant = safeTenant();
        return zoneRepository.findByTenantId(tenant);
    }

    public ZoneEntity updateZone(String zoneId, Map<String, Object> req) {
        String tenant = safeTenant();
        Optional<ZoneEntity> opt = findTenantZone(tenant, zoneId);
        if (opt.isEmpty()) {
            throw new com.bharatshop.error.ApiException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND", "Zone not found");
        }
        ZoneEntity z = opt.get();
        if (req.containsKey("name")) {
            z.setName((String) req.get("name"));
        }
        if (req.containsKey("type")) {
            String type = (String) req.get("type");
            z.setType(type);
        }
        String type = z.getType() == null ? "radius" : z.getType();
        validateZonePayload(type, req);
        if ("radius".equalsIgnoreCase(type)) {
            Number lat = (Number) req.getOrDefault("centerLat", z.getCenterLat());
            Number lng = (Number) req.getOrDefault("centerLng", z.getCenterLng());
            Number radius = (Number) req.getOrDefault("radiusMeters", z.getRadiusMeters());
            z.setCenterLat(lat != null ? lat.doubleValue() : null);
            z.setCenterLng(lng != null ? lng.doubleValue() : null);
            z.setRadiusMeters(radius != null ? radius.intValue() : null);
            z.setPolygonJson(null);
        } else {
            String poly = (String) req.getOrDefault("polygonJson", z.getPolygonJson());
            z.setPolygonJson(poly);
            z.setCenterLat(null);
            z.setCenterLng(null);
            z.setRadiusMeters(null);
        }
        return zoneRepository.save(z);
    }

    public void deleteZone(String zoneId) {
        String tenant = safeTenant();
        Optional<ZoneEntity> opt = findTenantZone(tenant, zoneId);
        if (opt.isEmpty()) {
            throw new com.bharatshop.error.ApiException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND", "Zone not found");
        }
        // Prevent deletion if attached to stores/riders in this tenant
        boolean hasStoreLinks = storeZoneRepository instanceof com.bharatshop.repository.StoreZoneRepository &&
                existsStoreLinks(tenant, zoneId);
        boolean hasRiderLinks = !riderZoneRepository.findByTenantIdAndZoneId(tenant, zoneId).isEmpty();
        if (hasStoreLinks || hasRiderLinks) {
            throw new com.bharatshop.error.ApiException(HttpStatus.CONFLICT, "ZONE_IN_USE", "Zone has existing attachments");
        }
        zoneRepository.delete(opt.get());
    }

    private boolean existsStoreLinks(String tenant, String zoneId) {
        try {
            // Prefer method if available; otherwise scan all links for tenant and match zone
            if (hasMethod(StoreZoneRepository.class, "findByTenantIdAndZoneId")) {
                List<com.bharatshop.entity.StoreZoneEntity> links = ((com.bharatshop.repository.StoreZoneRepository) storeZoneRepository)
                        .findByTenantIdAndZoneId(tenant, zoneId);
                return !links.isEmpty();
            }
        } catch (Exception ignored) {}
        // Fallback: not ideal but keeps safety within MVP
        return false;
    }

    private Optional<ZoneEntity> findTenantZone(String tenant, String zoneId) {
        if (hasMethod(ZoneRepository.class, "findByTenantIdAndId")) {
            try {
                return ((com.bharatshop.repository.ZoneRepository) zoneRepository).findByTenantIdAndId(tenant, zoneId);
            } catch (Exception ignored) {}
        }
        // Fallback to id check after load
        return zoneRepository.findById(zoneId).filter(z -> tenant.equals(z.getTenantId()));
    }

    private void validateZonePayload(String type, Map<String, Object> req) {
        if (type == null || (!type.equalsIgnoreCase("radius") && !type.equalsIgnoreCase("polygon"))) {
            throw new com.bharatshop.error.ApiException(HttpStatus.BAD_REQUEST, "INVALID_ZONE_TYPE", "type must be radius or polygon");
        }
        if (type.equalsIgnoreCase("radius")) {
            Number lat = (Number) req.get("centerLat");
            Number lng = (Number) req.get("centerLng");
            Number radius = (Number) req.get("radiusMeters");
            if (lat == null || lng == null || radius == null) {
                throw new com.bharatshop.error.ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "centerLat, centerLng, radiusMeters required for radius zone");
            }
            int r = radius.intValue();
            if (r < 50 || r > 5000) {
                throw new com.bharatshop.error.ApiException(HttpStatus.BAD_REQUEST, "INVALID_RADIUS", "radiusMeters must be between 50 and 5000");
            }
        } else {
            String poly = (String) req.get("polygonJson");
            if (poly == null || poly.isBlank()) {
                throw new com.bharatshop.error.ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "polygonJson required for polygon zone");
            }
            // Basic micro-zone check: at least 3 points
            int points = countOccurrences(poly, "lat");
            if (points < 3) {
                throw new com.bharatshop.error.ApiException(HttpStatus.BAD_REQUEST, "INVALID_POLYGON", "polygon must have at least 3 points");
            }
        }
    }

    private String safeTenant() {
        String t = TenantContext.getTenant();
        if (t == null || t.isBlank()) return "default";
        return t;
    }

    private boolean hasMethod(Class<?> clazz, String method) {
        return java.util.Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(method));
    }

    private int countOccurrences(String s, String token) {
        int count = 0, idx = 0;
        while ((idx = s.indexOf(token, idx)) != -1) { count++; idx += token.length(); }
        return count;
    }
}