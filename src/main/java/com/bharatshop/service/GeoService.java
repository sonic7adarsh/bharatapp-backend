package com.bharatshop.service;

import com.bharatshop.entity.ZoneEntity;
import org.springframework.stereotype.Service;

@Service
public class GeoService {
    public boolean isPointInZone(double lat, double lng, ZoneEntity zone) {
        if (zone == null) return false;
        if ("radius".equalsIgnoreCase(zone.getType())) {
            double d = haversine(lat, lng, zone.getCenterLat(), zone.getCenterLng());
            return d * 1000 <= (zone.getRadiusMeters() == null ? 0 : zone.getRadiusMeters());
        }
        // polygon support: polygonJson is expected as JSON array of {lat,lng}
        if (zone.getPolygonJson() == null || zone.getPolygonJson().isBlank()) return false;
        try {
            java.util.List<double[]> points = new java.util.ArrayList<>();
            com.fasterxml.jackson.databind.JsonNode arr = new com.fasterxml.jackson.databind.ObjectMapper().readTree(zone.getPolygonJson());
            if (arr.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                    double plat = n.has("lat") ? n.get("lat").asDouble() : n.get("x").asDouble();
                    double plng = n.has("lng") ? n.get("lng").asDouble() : n.get("y").asDouble();
                    points.add(new double[]{plat, plng});
                }
                return pointInPolygon(lat, lng, points);
            }
        } catch (Exception ignore) {}
        return false;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Ray casting algorithm for point-in-polygon
    private boolean pointInPolygon(double lat, double lng, java.util.List<double[]> polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0], yj = polygon.get(j)[1];
            boolean intersect = ((yi > lng) != (yj > lng)) &&
                    (lat < (xj - xi) * (lng - yi) / (yj - yi + 0.0) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}