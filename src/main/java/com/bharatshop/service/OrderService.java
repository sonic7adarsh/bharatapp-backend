package com.bharatshop.service;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.CartItem;
import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.entity.OrderItemEntity;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.service.InventoryService;
import com.bharatshop.service.NotificationService;
import com.bharatshop.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final StoreRepository storeRepository;
    private final TenantConfigurationService tenantConfigurationService;
    @org.springframework.beans.factory.annotation.Value("${app.orders.acceptanceWindowMinutes:15}")
    private int acceptanceWindowMinutes;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        InventoryService inventoryService, NotificationService notificationService,
                        StoreRepository storeRepository, TenantConfigurationService tenantConfigurationService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.storeRepository = storeRepository;
        this.tenantConfigurationService = tenantConfigurationService;
    }

    public Order placeOrder(String userId, List<CartItem> items, Order.Totals totals, String paymentMethod, Order.PaymentInfo paymentInfo, String type, String storeId, String notes) {
        String id = UUID.randomUUID().toString();
        OrderEntity e = new OrderEntity();
        e.setId(id);
        e.setTenantId(com.bharatshop.tenant.TenantContext.getTenant());
        e.setReference("REF-" + id.substring(0, 8));
        e.setUserId(userId);
        e.setStatus("placed");
        e.setPaymentMethod(paymentMethod);
        e.setType(type == null ? "order" : type);
        e.setCreatedAt(Instant.now());
        e.setStoreId(storeId);
        e.setNotes(notes);
        if (acceptanceWindowMinutes > 0) {
            e.setSellerResponseDeadline(Instant.now().plusSeconds(acceptanceWindowMinutes * 60L));
        }
        double total = totals != null && totals.payable != null ? totals.payable :
                items == null ? 0 : items.stream().mapToDouble(ci -> ci.getPrice() * ci.getQuantity()).sum();
        e.setTotal(total);
        orderRepository.save(e);

        if (items != null) {
            List<OrderItemEntity> persist = new ArrayList<>();
            for (CartItem ci : items) {
                OrderItemEntity oi = new OrderItemEntity();
                oi.setId(UUID.randomUUID().toString());
                oi.setOrderId(id);
                oi.setProductId(ci.getId());
                oi.setName(ci.getName());
                oi.setPrice(ci.getPrice());
                oi.setQuantity(ci.getQuantity());
                oi.setRequiresPrescription(ci.getRequiresPrescription());
                persist.add(oi);
            }
            orderItemRepository.saveAll(persist);
        }

        // Atomic reserve at checkout: reserve all items or rollback entirely
        if (items != null && !items.isEmpty()) {
            String tenantId = e.getTenantId();
            java.util.List<java.util.Map.Entry<String, Integer>> reserved = new java.util.ArrayList<>();
            for (CartItem ci : items) {
                if (ci.getId() == null || ci.getQuantity() <= 0) continue;
                boolean ok = inventoryService.reserve(tenantId, ci.getId(), ci.getQuantity());
                if (!ok) {
                    // rollback any prior reservations
                    for (java.util.Map.Entry<String, Integer> r : reserved) {
                        try { inventoryService.release(tenantId, r.getKey(), r.getValue()); } catch (Exception ex) { }
                    }
                    // set order to cancelled due to insufficient inventory
                    e.setStatus("cancelled");
                    e.setCancelledAt(Instant.now());
                    e.setCancellationReason("insufficient_inventory_checkout");
                    orderRepository.save(e);
                    throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY",
                            "Insufficient stock for one or more items",
                            Map.of("orderId", e.getId()));
                }
                reserved.add(new java.util.AbstractMap.SimpleEntry<>(ci.getId(), ci.getQuantity()));
            }
        }

        // Send order placed notification (buyer)
        try {
            notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "PLACED",
                    storeId != null ? Map.of("storeId", storeId) : null);
        } catch (Exception ex) {
            // Ignore notification errors to not block order placement
        }

        // Notify seller of new order
        try {
            if (storeId != null) {
                java.util.Optional<StoreEntity> storeOpt = storeRepository.findById(storeId);
                if (storeOpt.isPresent() && storeOpt.get().getOwnerId() != null) {
                    String sellerId = storeOpt.get().getOwnerId();
                    notificationService.sendSellerNotification(e.getTenantId(), sellerId, "NEW_ORDER",
                            Map.of("orderId", e.getId(), "storeId", storeId));
                }
            }
        } catch (Exception ex) {
            // Do not block order placement on seller notification failures
        }

        Order dto = toDto(e);
        dto.setItems(items);
        dto.setTotals(totals);
        dto.setPaymentInfo(paymentInfo);
        return dto;
    }

    public List<Order> listOrders(String userId) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        List<OrderEntity> entities = tenantId != null ? 
            orderRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId) :
            orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Instant now = Instant.now();
        for (OrderEntity e : entities) {
            // Auto-cancel when seller misses acceptance window
            if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null && e.getSellerResponseDeadline() != null && now.isAfter(e.getSellerResponseDeadline())) {
                e.setStatus("cancelled");
                e.setCancelledAt(now);
                if (e.getCancellationReason() == null || e.getCancellationReason().isBlank()) {
                    e.setCancellationReason("auto_cancelled_no_response");
                }
                orderRepository.save(e);
                // Release reserved inventory on auto-cancel
                List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
                for (OrderItemEntity oi : items) {
                    if (oi.getProductId() != null) {
                        inventoryService.release(e.getTenantId(), oi.getProductId(), oi.getQuantity());
                    }
                }
                // Notify buyer
                try {
                    notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "CANCELLED",
                            Map.of("reason", "auto_cancelled_no_response"));
                } catch (Exception ex) { }
            }

            // Enforce reservation TTL: auto-cancel if order remains unaccepted beyond TTL
            if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null) {
                String tenantIdCfg = e.getTenantId();
                int ttlMinutes = tenantConfigurationService.getInventoryReservationTimeout(tenantIdCfg);
                Instant expiry = e.getCreatedAt() != null ? e.getCreatedAt().plusSeconds(ttlMinutes * 60L) : null;
                if (expiry != null && now.isAfter(expiry)) {
                    e.setStatus("cancelled");
                    e.setCancelledAt(now);
                    e.setCancellationReason("auto_cancelled_reservation_timeout");
                    orderRepository.save(e);
                    // Release reserved inventory
                    List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
                    for (OrderItemEntity oi : items) {
                        if (oi.getProductId() != null) {
                            inventoryService.release(e.getTenantId(), oi.getProductId(), oi.getQuantity());
                        }
                    }
                    // Notify buyer
                    try {
                        notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "CANCELLED",
                                Map.of("reason", "auto_cancelled_reservation_timeout"));
                    } catch (Exception ex) { }
                }
            }
        }
        return entities.stream().map(this::toDtoWithItems).collect(Collectors.toList());
    }

    public Order cancelOrderByAdmin(String tenantId, String orderId, String reason) {
        String t = tenantId != null && !tenantId.isBlank() ? tenantId : com.bharatshop.tenant.TenantContext.getTenant();
        java.util.Optional<OrderEntity> opt = t != null ? orderRepository.findByTenantIdAndId(t, orderId) : orderRepository.findById(orderId);
        if (opt.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
        }
        OrderEntity e = opt.get();
        if ("cancelled".equalsIgnoreCase(e.getStatus())) {
            return toDto(e);
        }
        e.setStatus("cancelled");
        e.setCancelledAt(Instant.now());
        if (reason != null && !reason.isBlank()) { e.setCancellationReason(reason); }
        orderRepository.save(e);

        // Release reserved inventory on cancel
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
        for (OrderItemEntity oi : items) {
            if (oi.getProductId() != null) {
                inventoryService.release(e.getTenantId(), oi.getProductId(), oi.getQuantity());
            }
        }

        // Notify buyer and seller
        try { notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "CANCELLED", null); } catch (Exception ignore) {}
        try {
            if (e.getStoreId() != null) {
                java.util.Optional<StoreEntity> storeOpt = storeRepository.findById(e.getStoreId());
                if (storeOpt.isPresent() && storeOpt.get().getOwnerId() != null) {
                    notificationService.sendSellerNotification(e.getTenantId(), storeOpt.get().getOwnerId(), "ORDER_CANCELLED",
                            java.util.Map.of("orderId", e.getId(), "storeId", e.getStoreId()));
                }
            }
        } catch (Exception ignore) {}

        return toDto(e);
    }



    public Order cancelOrderByUser(String userId, String orderId, String reason) {
        String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
        java.util.Optional<OrderEntity> opt = tenantId != null && !tenantId.isBlank()
                ? orderRepository.findByTenantIdAndId(tenantId, orderId)
                : orderRepository.findById(orderId);
        return opt
                .filter(e -> Objects.equals(e.getUserId(), userId))
                .map(e -> {
                    String cur = e.getStatus() == null ? "" : e.getStatus().toUpperCase();
                    if ("READY".equals(cur) || "SHIPPED".equals(cur) || "DELIVERED".equals(cur)) {
                        throw new ApiException(HttpStatus.CONFLICT, "CANCEL_NOT_ALLOWED", "Cancellation not allowed after READY");
                    }
                    if ("CANCELLED".equals(cur)) {
                        return toDtoWithItems(e);
                    }
                    e.setStatus("cancelled");
                    e.setCancelledAt(Instant.now());
                    if (reason != null && !reason.isBlank()) { e.setCancellationReason(reason); }
                    orderRepository.save(e);
                    // Release reserved inventory on cancellation
                    List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
                    for (OrderItemEntity oi : items) {
                        if (oi.getProductId() != null) {
                            inventoryService.release(e.getTenantId(), oi.getProductId(), oi.getQuantity());
                        }
                    }
                    // Send order cancelled notification
                    try {
                        notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "CANCELLED",
                                reason != null ? Map.of("reason", reason) : null);
                    } catch (Exception ex) { }
                    return toDtoWithItems(e);
                })
                .orElse(null);
    }

    public java.util.Optional<com.yourapp.dto.CustomerOrderDto> getOrderForCustomer(String orderId, String userId, String tenant) {
        java.util.Optional<OrderEntity> opt = orderRepository.findByIdAndUserIdAndTenantId(orderId, userId, tenant);
        return opt.map(this::toCustomerDtoWithItems);
    }

    public void cancelOrderForCustomer(String orderId, String userId, String tenant) {
        java.util.Optional<OrderEntity> opt = orderRepository.findByIdAndUserIdAndTenantId(orderId, userId, tenant);
        if (opt.isEmpty()) {
            throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
        }
        OrderEntity e = opt.get();
        String cur = e.getStatus() == null ? "" : e.getStatus().toUpperCase();
        boolean acceptedOrBeyond = e.getSellerAcceptedAt() != null || (!"PLACED".equals(cur));
        if (acceptedOrBeyond) {
            throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.CONFLICT, "CANCEL_NOT_ALLOWED", "Cancellation not allowed after ACCEPTED");
        }
        // Only allowed when status == PLACED
        e.setStatus("cancelled");
        e.setCancelledAt(java.time.Instant.now());
        e.setCancellationReason("customer_cancelled");
        orderRepository.save(e);
        // Release reserved inventory on cancellation
        java.util.List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
        for (OrderItemEntity oi : items) {
            if (oi.getProductId() != null) {
                inventoryService.release(e.getTenantId(), oi.getProductId(), oi.getQuantity());
            }
        }
        // Send order cancelled notification (buyer)
        try {
            notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "CANCELLED",
                    java.util.Map.of("reason", "customer_cancelled"));
        } catch (Exception ignore) {}
    }

    public java.util.List<com.yourapp.dto.CustomerOrderDto> getOrdersForCustomer(String userId, String tenant) {
        java.util.List<OrderEntity> entities = tenant != null && !tenant.isBlank()
                ? orderRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenant, userId)
                : orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return entities.stream().map(this::toCustomerDtoWithItems).collect(java.util.stream.Collectors.toList());
    }

    private com.yourapp.dto.CustomerOrderDto toCustomerDto(OrderEntity e) {
        com.yourapp.dto.CustomerOrderDto dto = new com.yourapp.dto.CustomerOrderDto();
        dto.setId(e.getId());
        dto.setStatus(e.getStatus());
        dto.setTotal(e.getTotal());
        dto.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        com.yourapp.dto.CustomerOrderDto.Store store = new com.yourapp.dto.CustomerOrderDto.Store();
        store.setId(e.getStoreId());
        try {
            if (e.getStoreId() != null) {
                java.util.Optional<com.bharatshop.entity.StoreEntity> s = storeRepository.findById(e.getStoreId());
                store.setName(s.isPresent() ? s.get().getName() : null);
            }
        } catch (Exception ignored) {}
        dto.setStore(store);
        return dto;
    }

    private com.yourapp.dto.CustomerOrderDto toCustomerDtoWithItems(OrderEntity e) {
        com.yourapp.dto.CustomerOrderDto dto = toCustomerDto(e);
        java.util.List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
        java.util.List<com.yourapp.dto.CustomerOrderDto.Item> out = new java.util.ArrayList<>();
        for (com.bharatshop.entity.OrderItemEntity oi : items) {
            com.yourapp.dto.CustomerOrderDto.Item mi = new com.yourapp.dto.CustomerOrderDto.Item();
            mi.setProductId(oi.getProductId());
            mi.setName(oi.getName());
            mi.setQuantity(oi.getQuantity());
            out.add(mi);
        }
        dto.setItems(out);
        return dto;
    }

    public Order toDto(OrderEntity e) {
        Order o = new Order();
        o.setId(e.getId());
        o.setReference(e.getReference());
        o.setStatus(e.getStatus());
        o.setTotal(e.getTotal());
        o.setPaymentMethod(e.getPaymentMethod());
        o.setType(e.getType());
        o.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        o.setSellerResponseDeadline(e.getSellerResponseDeadline() != null ? e.getSellerResponseDeadline().toString() : null);
        o.setSellerAcceptedAt(e.getSellerAcceptedAt() != null ? e.getSellerAcceptedAt().toString() : null);
        o.setCancelledAt(e.getCancelledAt() != null ? e.getCancelledAt().toString() : null);
        o.setCancellationReason(e.getCancellationReason());
        o.setStoreId(e.getStoreId());
        o.setNotes(e.getNotes());
        Order.Totals t = new Order.Totals();
        t.payable = e.getTotal();
        o.setTotals(t);
        return o;
    }

    private Order toDtoWithItems(OrderEntity e) {
        Order o = toDto(e);
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
        List<CartItem> cartItems = items.stream().map(this::toCartItem).collect(Collectors.toList());
        o.setItems(cartItems);
        return o;
    }

    private CartItem toCartItem(OrderItemEntity oi) {
        CartItem ci = new CartItem();
        ci.setId(oi.getProductId());
        ci.setName(oi.getName());
        ci.setPrice(oi.getPrice());
        ci.setQuantity(oi.getQuantity());
        ci.setRequiresPrescription(oi.getRequiresPrescription());
        ci.setStatus(oi.getStatus());
        return ci;
    }
}