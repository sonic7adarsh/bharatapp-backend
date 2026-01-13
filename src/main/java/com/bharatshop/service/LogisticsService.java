package com.bharatshop.service;

import com.bharatshop.entity.OrderDeliveryEntity;
import com.bharatshop.entity.DeliveryAttemptEntity;
import com.bharatshop.entity.RiderEntity;
import com.bharatshop.entity.StoreZoneEntity;
import com.bharatshop.entity.RiderZoneEntity;
import com.bharatshop.entity.RiderLocationEntity;
import com.bharatshop.repository.OrderDeliveryRepository;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.repository.RiderRepository;
import com.bharatshop.repository.DeliveryAttemptRepository;
import com.bharatshop.repository.StoreZoneRepository;
import com.bharatshop.repository.RiderZoneRepository;
import com.bharatshop.repository.RiderLocationRepository;
import com.bharatshop.service.GeoService;
import com.bharatshop.service.NotificationService;
import com.bharatshop.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.Optional;
import java.util.Comparator;

@Service
public class LogisticsService {
    private final RiderRepository riderRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final StoreZoneRepository storeZoneRepository;
    private final RiderZoneRepository riderZoneRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final RiderLocationRepository riderLocationRepository;
    private final GeoService geoService;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    private final OrderItemRepository orderItemRepository;

    public LogisticsService(RiderRepository riderRepository,
                            OrderDeliveryRepository orderDeliveryRepository,
                            StoreZoneRepository storeZoneRepository,
                            RiderZoneRepository riderZoneRepository,
                            DeliveryAttemptRepository deliveryAttemptRepository,
                            RiderLocationRepository riderLocationRepository,
                            GeoService geoService,
                            OrderRepository orderRepository,
                            NotificationService notificationService,
                            InventoryService inventoryService,
                            OrderItemRepository orderItemRepository) {
        this.riderRepository = riderRepository;
        this.orderDeliveryRepository = orderDeliveryRepository;
        this.storeZoneRepository = storeZoneRepository;
        this.riderZoneRepository = riderZoneRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.riderLocationRepository = riderLocationRepository;
        this.geoService = geoService;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.inventoryService = inventoryService;
        this.orderItemRepository = orderItemRepository;
    }

    public OrderDeliveryEntity assignRider(String tenantId, String orderId, String storeId) {
        // Guard: Order must be READY
        var orderOpt = orderRepository.findByTenantIdAndId(tenantId, orderId);
        if (orderOpt.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
        }
        String curStatus = orderOpt.get().getStatus() == null ? "" : orderOpt.get().getStatus().toUpperCase();
        if (!"READY".equals(curStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Cannot perform this action in current order state");
        }
        List<RiderEntity> available = riderRepository.findByTenantIdAndStatus(tenantId, "ONLINE");
        if (available.isEmpty()) { return null; }

        // Fetch zones served by the store
        List<StoreZoneEntity> storeZoneLinks = storeZoneRepository.findByTenantIdAndStoreId(tenantId, storeId);
        Set<String> storeZoneIds = storeZoneLinks.stream().map(StoreZoneEntity::getZoneId).collect(Collectors.toSet());

        // Get store zone center coordinates for distance calculation
        double storeLat = 0, storeLng = 0;
        if (!storeZoneIds.isEmpty()) {
            String firstZoneId = storeZoneIds.iterator().next();
            // We need to get zone entity - let's assume we have a method to get zone by id
            // For now, we'll use a simplified approach
        }

        // Prefer riders whose currentZoneId matches any store zone
        List<RiderEntity> zoneMatchedRiders = available.stream()
                .filter(r -> r.getCurrentZoneId() != null && storeZoneIds.contains(r.getCurrentZoneId()))
                .collect(Collectors.toList());

        // If zone-matched riders exist, sort by distance
        if (!zoneMatchedRiders.isEmpty()) {
            zoneMatchedRiders.sort(Comparator.comparingDouble(rider -> {
                List<RiderLocationEntity> locs = riderLocationRepository.findByTenantIdAndRiderId(tenantId, rider.getId());
                if (!locs.isEmpty()) {
                    RiderLocationEntity loc = locs.stream()
                            .max(Comparator.comparing(RiderLocationEntity::getUpdatedAt))
                            .orElse(locs.get(0));
                    // Simplified distance metric as a placeholder until zone center is available
                    return Math.abs(loc.getLat()) + Math.abs(loc.getLng());
                }
                return Double.MAX_VALUE;
            }));
            RiderEntity rider = zoneMatchedRiders.get(0);
            
            rider.setStatus("ASSIGNED");
            riderRepository.save(rider);

            OrderDeliveryEntity delivery = new OrderDeliveryEntity();
            delivery.setId(UUID.randomUUID().toString());
            delivery.setTenantId(tenantId);
            delivery.setOrderId(orderId);
            delivery.setStoreId(storeId);
            delivery.setRiderId(rider.getId());
            delivery.setStatus("RIDER_ASSIGNED");
            delivery.setAssignedAt(Instant.now());
            delivery.setOtp(String.valueOf((int)(Math.random()*9000)+1000));
            return orderDeliveryRepository.save(delivery);
        }

        // If none match by currentZoneId, prefer riders mapped to the store zones
        if (!storeZoneIds.isEmpty()) {
            for (RiderEntity r : available) {
                List<RiderZoneEntity> rZones = riderZoneRepository.findByTenantIdAndRiderId(tenantId, r.getId());
                boolean matches = rZones.stream().anyMatch(z -> storeZoneIds.contains(z.getZoneId()));
                if (matches) { 
                    r.setStatus("ASSIGNED");
                    riderRepository.save(r);

                    OrderDeliveryEntity delivery = new OrderDeliveryEntity();
                    delivery.setId(UUID.randomUUID().toString());
                    delivery.setTenantId(tenantId);
                    delivery.setOrderId(orderId);
                    delivery.setStoreId(storeId);
                    delivery.setRiderId(r.getId());
                    delivery.setStatus("RIDER_ASSIGNED");
                    delivery.setAssignedAt(Instant.now());
                    delivery.setOtp(String.valueOf((int)(Math.random()*9000)+1000));
                    return orderDeliveryRepository.save(delivery);
                }
            }
        }

        // Fallback to first available rider if no zone match
        RiderEntity rider = available.get(0);
        rider.setStatus("ASSIGNED");
        riderRepository.save(rider);

        OrderDeliveryEntity delivery = new OrderDeliveryEntity();
        delivery.setId(UUID.randomUUID().toString());
        delivery.setTenantId(tenantId);
        delivery.setOrderId(orderId);
        delivery.setStoreId(storeId);
        delivery.setRiderId(rider.getId());
        delivery.setStatus("RIDER_ASSIGNED");
        delivery.setAssignedAt(Instant.now());
        delivery.setOtp(String.valueOf((int)(Math.random()*9000)+1000));
        return orderDeliveryRepository.save(delivery);
    }

    /**
     * Admin override: assign rider optionally explicitly. Enforces order READY and tenant scoping.
     */
    public OrderDeliveryEntity assignRiderAdmin(String tenantId, String orderId, String storeId, String riderId) {
        var orderOpt = orderRepository.findByTenantIdAndId(tenantId, orderId);
        if (orderOpt.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
        }
        String curStatus = orderOpt.get().getStatus() == null ? "" : orderOpt.get().getStatus().toUpperCase();
        if (!"READY".equals(curStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Cannot perform this action in current order state");
        }
        if (riderId == null || riderId.isBlank()) {
            // Fallback to existing auto-assignment logic
            return assignRider(tenantId, orderId, storeId);
        }
        RiderEntity rider = riderRepository.findById(riderId).orElse(null);
        if (rider == null || rider.getTenantId() == null || !tenantId.equals(rider.getTenantId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CROSS_TENANT_RIDER", "Rider not in current tenant");
        }
        if (!"ONLINE".equalsIgnoreCase(rider.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "RIDER_NOT_AVAILABLE", "Rider not available for assignment");
        }
        rider.setStatus("ASSIGNED");
        riderRepository.save(rider);

        OrderDeliveryEntity delivery = new OrderDeliveryEntity();
        delivery.setId(UUID.randomUUID().toString());
        delivery.setTenantId(tenantId);
        delivery.setOrderId(orderId);
        delivery.setStoreId(storeId);
        delivery.setRiderId(rider.getId());
        delivery.setStatus("RIDER_ASSIGNED");
        delivery.setAssignedAt(Instant.now());
        delivery.setOtp(String.valueOf((int)(Math.random()*9000)+1000));
        return orderDeliveryRepository.save(delivery);
    }

    /**
     * Admin override: unassign rider from a delivery if not yet picked up.
     */
    public OrderDeliveryEntity unassignRiderAdmin(String tenantId, String deliveryId) {
        OrderDeliveryEntity d = orderDeliveryRepository.findByTenantIdAndId(tenantId, deliveryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "Delivery not found"));
        String status = d.getStatus() == null ? "" : d.getStatus().toUpperCase();
        if (!"RIDER_ASSIGNED".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Can only unassign when RIDER_ASSIGNED");
        }
        var orderOpt = orderRepository.findByTenantIdAndId(tenantId, d.getOrderId());
        if (orderOpt.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
        }
        String orderStatus = orderOpt.get().getStatus() == null ? "" : orderOpt.get().getStatus().toUpperCase();
        if (!"READY".equals(orderStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Order must be READY to unassign rider");
        }
        // Reset rider status to ONLINE
        if (d.getRiderId() != null) {
            riderRepository.findById(d.getRiderId()).ifPresent(r -> {
                if (tenantId.equals(r.getTenantId())) {
                    r.setStatus("ONLINE");
                    riderRepository.save(r);
                }
            });
        }
        // Reset delivery to pending assignment
        d.setRiderId(null);
        d.setStatus("PENDING");
        d.setAssignedAt(null);
        d.setOtp(null);
        return orderDeliveryRepository.save(d);
    }

    public OrderDeliveryEntity markPickedUp(String deliveryId) {
        return orderDeliveryRepository.findById(deliveryId).map(d -> {
            String cur = d.getStatus() == null ? "" : d.getStatus();
            if (!"RIDER_ASSIGNED".equalsIgnoreCase(cur)) {
                throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Cannot perform this action in current order state");
            }
            d.setStatus("PICKED_UP");
            d.setPickedUpAt(Instant.now());
            return orderDeliveryRepository.save(d);
        }).orElse(null);
    }

    public OrderDeliveryEntity markOutForDelivery(String deliveryId) {
        return orderDeliveryRepository.findById(deliveryId).map(d -> {
            String cur = d.getStatus() == null ? "" : d.getStatus();
            if (!"PICKED_UP".equalsIgnoreCase(cur)) {
                throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Cannot perform this action in current order state");
            }
            d.setStatus("OUT_FOR_DELIVERY");
            return orderDeliveryRepository.save(d);
        }).orElse(null);
    }

    public OrderDeliveryEntity completeWithOtp(String deliveryId, String otp) {
        return orderDeliveryRepository.findById(deliveryId).map(d -> {
            String cur = d.getStatus() == null ? "" : d.getStatus();
            if (!"OUT_FOR_DELIVERY".equalsIgnoreCase(cur)) {
                throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Cannot perform this action in current order state");
            }
            if (d.getOtp() == null || !d.getOtp().equals(otp)) {
                throw new ApiException(HttpStatus.CONFLICT, "INVALID_OTP", "Incorrect OTP");
            }
            d.setStatus("DELIVERED");
            d.setCompletedAt(Instant.now());
            // Sync order status and notify buyer
            orderRepository.findByTenantIdAndId(d.getTenantId(), d.getOrderId()).ifPresent(order -> {
                order.setStatus("delivered");
                orderRepository.save(order);
                // Consume reserved inventory for all order items
                try {
                    java.util.List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
                    for (com.bharatshop.entity.OrderItemEntity oi : items) {
                        if (oi.getProductId() != null && oi.getQuantity() > 0) {
                            inventoryService.consume(order.getTenantId(), oi.getProductId(), oi.getQuantity());
                        }
                    }
                } catch (Exception ignore) {}
                try {
                    notificationService.sendOrderNotification(order.getTenantId(), order.getUserId(), order.getId(), "DELIVERED",
                            java.util.Map.of("storeId", d.getStoreId()));
                } catch (Exception ignored) {}
            });
            return orderDeliveryRepository.save(d);
        }).orElse(null);
    }

    public DeliveryAttemptEntity recordAttempt(String tenantId, String deliveryId, String status, String note) {
        DeliveryAttemptEntity a = new DeliveryAttemptEntity();
        a.setId(java.util.UUID.randomUUID().toString());
        a.setTenantId(tenantId);
        a.setDeliveryId(deliveryId);
        a.setStatus(status);
        a.setNote(note);
        a.setTs(Instant.now());
        DeliveryAttemptEntity saved = deliveryAttemptRepository.save(a);
        return saved;
    }

    public java.util.List<DeliveryAttemptEntity> getAttempts(String tenantId, String deliveryId) {
        return deliveryAttemptRepository.findByTenantIdAndDeliveryId(tenantId, deliveryId);
    }

    public java.util.Optional<OrderDeliveryEntity> findByTenantIdAndId(String tenantId, String id) {
        return orderDeliveryRepository.findByTenantIdAndId(tenantId, id);
    }
}