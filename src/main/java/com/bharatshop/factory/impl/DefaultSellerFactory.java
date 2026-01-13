package com.bharatshop.factory.impl;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.factory.SellerFactory;
import com.bharatshop.factory.ops.SellerOrderOps;
import com.bharatshop.factory.ops.SellerProductOps;
import com.bharatshop.factory.ops.SellerStoreOps;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.repository.ProductRepository;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.service.OrderService;
import com.bharatshop.service.ProductService;
import com.bharatshop.service.StoreService;
import com.bharatshop.service.InventoryService;
import com.bharatshop.service.NotificationService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DefaultSellerFactory implements SellerFactory {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreService storeService;
    private final StoreRepository storeRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public DefaultSellerFactory(ProductService productService,
                                ProductRepository productRepository,
                                OrderService orderService,
                                OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository,
                                StoreService storeService,
                                StoreRepository storeRepository,
                                InventoryService inventoryService,
                                NotificationService notificationService) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.storeService = storeService;
        this.storeRepository = storeRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    private Instant parseDayStart(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            String v = value.trim();
            if (v.matches("\\d{4}-\\d{2}-\\d{2}")) {
                java.time.LocalDate d = java.time.LocalDate.parse(v);
                return d.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            }
            return Instant.parse(v);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public SellerProductOps products() {
        return new SellerProductOps() {
            @Override
            public List<Product> listByStore(String storeId, String search, String category, Boolean active, Integer page, Integer limit) {
                List<Product> all = productService.getByStore(storeId);
                if (search != null && !search.isBlank()) {
                    String s = search.toLowerCase();
                    all = all.stream().filter(p -> p.getName() != null && p.getName().toLowerCase().contains(s)).collect(Collectors.toList());
                }
                if (category != null && !category.isBlank()) {
                    String c = category.toLowerCase();
                    all = all.stream().filter(p -> p.getCategory() != null && p.getCategory().toLowerCase().equals(c)).collect(Collectors.toList());
                }
                if (active != null) {
                    all = all.stream().filter(p -> Objects.equals(p.getActive(), active)).collect(Collectors.toList());
                }
                if (limit != null && limit > 0) {
                    int p = page == null || page < 1 ? 1 : page;
                    int from = (p - 1) * limit;
                    if (from < all.size()) all = all.subList(from, Math.min(from + limit, all.size()));
                    else all = Collections.emptyList();
                }
                return all;
            }

            @Override
            public Product create(String storeId, Product product) {
                product.setStoreId(storeId);
                productService.add(product);
                return product;
            }

            @Override
            public Product updatePartial(String id, Map<String, Object> changes) {
                return productService.updatePartial(id, changes);
            }

            @Override
            public boolean delete(String id, boolean archive) {
                if (archive) {
                    Map<String, Object> change = new HashMap<>();
                    change.put("active", false);
                    productService.updatePartial(id, change);
                    return true;
                }
                return productRepository.findById(id).map(entity -> {
                    productRepository.delete(entity);
                    return true;
                }).orElse(false);
            }

            @Override
            public Product updateImage(String id, String filename) {
                return productService.updateImage(id, filename);
            }

            @Override
            public Product adjustInventory(String id, Integer stockDelta, Integer stockSet, Double price) {
                return productService.adjustInventory(id, stockDelta, stockSet, price);
            }
        };
    }

    @Override
    public SellerOrderOps orders() {
        return new SellerOrderOps() {
            @Override
            public List<Order> list(String storeId, String status, String from, String to, Integer page, Integer limit) {
                List<com.bharatshop.entity.OrderEntity> entities = orderRepository.findAll();
                // Scope to tenant and seller-owned stores
                String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
                var principal = com.bharatshop.security.UserPrincipal.current();
                java.util.Set<String> ownedStoreIds = new java.util.HashSet<>();
                if (principal != null) {
                    String ownerId = principal.getUserId();
                    List<com.bharatshop.entity.StoreEntity> ownedStores = storeRepository.findByOwnerId(ownerId);
                    for (com.bharatshop.entity.StoreEntity se : ownedStores) {
                        if (se.getId() != null) ownedStoreIds.add(se.getId());
                    }
                }
                // Auto-cancel expired placed orders
                Instant now = Instant.now();
                for (com.bharatshop.entity.OrderEntity e : entities) {
                    if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null && e.getSellerResponseDeadline() != null && now.isAfter(e.getSellerResponseDeadline())) {
                        e.setStatus("cancelled");
                        e.setCancelledAt(now);
                        if (e.getCancellationReason() == null || e.getCancellationReason().isBlank()) {
                            e.setCancellationReason("auto_cancelled_no_response");
                        }
                        orderRepository.save(e);
                        // Release reserved inventory on auto-cancel
                        String eTenantId = e.getTenantId();
                        List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
                        for (com.bharatshop.entity.OrderItemEntity oi : items) {
                            if (oi.getProductId() != null) {
                                inventoryService.release(eTenantId, oi.getProductId(), oi.getQuantity());
                            }
                        }
                        // Notify buyer about auto-cancel
                        try {
                            notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "CANCELLED",
                                    Map.of("reason", "auto_cancelled_no_response"));
                        } catch (Exception ex) { }
                    }
                }
                // Filter by tenant and owned stores
                if (tenantId != null && !tenantId.isBlank()) {
                    String t = tenantId;
                    entities = entities.stream().filter(o -> t.equals(o.getTenantId())).collect(Collectors.toList());
                }
                if (!ownedStoreIds.isEmpty()) {
                    java.util.Set<String> ids = ownedStoreIds;
                    entities = entities.stream().filter(o -> o.getStoreId() != null && ids.contains(o.getStoreId())).collect(Collectors.toList());
                }
                if (storeId != null && !storeId.isBlank()) {
                    String sid = storeId;
                    entities = entities.stream().filter(o -> sid.equals(o.getStoreId())).collect(Collectors.toList());
                }

                Instant fromTs = parseDayStart(from);
                Instant toTs = parseDayStart(to);
                if (status != null && !status.isBlank()) {
                    String s = status.toLowerCase();
                    entities = entities.stream().filter(o -> o.getStatus() != null && o.getStatus().equalsIgnoreCase(s)).collect(Collectors.toList());
                }
                if (fromTs != null) {
                    Instant f = fromTs;
                    entities = entities.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(f)).collect(Collectors.toList());
                }
                if (toTs != null) {
                    Instant t = toTs;
                    entities = entities.stream().filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isAfter(t)).collect(Collectors.toList());
                }
                entities.sort(Comparator.comparing(com.bharatshop.entity.OrderEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                List<Order> dto = entities.stream().map(DefaultSellerFactory.this::toDto).collect(Collectors.toList());
                if (limit != null && limit > 0) {
                    int p = page == null || page < 1 ? 1 : page;
                    int fromIdx = (p - 1) * limit;
                    if (fromIdx < dto.size()) dto = dto.subList(fromIdx, Math.min(fromIdx + limit, dto.size()));
                    else dto = Collections.emptyList();
                }
                return dto;
            }

            @Override
            public Order get(String orderId) {
                return orderRepository.findById(orderId)
                        .map(e -> {
                            Instant now = Instant.now();
                            if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null && e.getSellerResponseDeadline() != null && now.isAfter(e.getSellerResponseDeadline())) {
                                e.setStatus("cancelled");
                                e.setCancelledAt(now);
                                if (e.getCancellationReason() == null || e.getCancellationReason().isBlank()) {
                                    e.setCancellationReason("auto_cancelled_no_response");
                                }
                                orderRepository.save(e);
                                // Release reserved inventory on auto-cancel
                                String tenantId = e.getTenantId();
                                List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
                                for (com.bharatshop.entity.OrderItemEntity oi : items) {
                                    if (oi.getProductId() != null) {
                                        inventoryService.release(tenantId, oi.getProductId(), oi.getQuantity());
                                    }
                                }
                                // Notify buyer
                                try {
                                    notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "CANCELLED",
                                            Map.of("reason", "auto_cancelled_no_response"));
                                } catch (Exception ex) { }
                            }
                            // Enforce tenant/store ownership scoping
                            String t = com.bharatshop.tenant.TenantContext.getTenant();
                            if (t != null && !t.isBlank() && !t.equals(e.getTenantId())) {
                                return null;
                            }
                            var principal = com.bharatshop.security.UserPrincipal.current();
                            if (principal != null) {
                                String ownerId = principal.getUserId();
                                List<com.bharatshop.entity.StoreEntity> ownedStores = storeRepository.findByOwnerId(ownerId);
                                boolean owns = ownedStores.stream().anyMatch(se -> se.getId() != null && se.getId().equals(e.getStoreId()));
                                if (!owns) return null;
                            }
                            return DefaultSellerFactory.this.toDto(e);
                        })
                        .orElse(null);
            }

            @Override
            public Order updateStatus(String orderId, String status, String notes) {
                return orderRepository.findById(orderId).map(entity -> {
                    // Enforce tenant scoping
                    String t = com.bharatshop.tenant.TenantContext.getTenant();
                    if (t != null && !t.isBlank() && !t.equals(entity.getTenantId())) {
                        throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "TENANT_MISMATCH", "Wrong tenant context", java.util.Map.of("orderId", entity.getId()));
                    }
                    // Enforce store ownership for current seller
                    var principal = com.bharatshop.security.UserPrincipal.current();
                    if (principal != null) {
                        String ownerId = principal.getUserId();
                        java.util.List<com.bharatshop.entity.StoreEntity> ownedStores = storeRepository.findByOwnerId(ownerId);
                        boolean owns = ownedStores.stream().anyMatch(se -> se.getId() != null && se.getId().equals(entity.getStoreId()));
                        if (!owns) {
                            throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "NOT_STORE_OWNER", "Seller does not own this store", java.util.Map.of("storeId", entity.getStoreId()));
                        }
                    }
                    String cur = entity.getStatus() != null ? entity.getStatus().toLowerCase() : "";
                    String desired = status != null ? status.toLowerCase() : "";

                    boolean valid = true;
                    switch (desired) {
                        case "accepted":
                            valid = "placed".equals(cur);
                            if (valid) {
                                // Reject acceptance if past seller response deadline
                                java.time.Instant now = java.time.Instant.now();
                                if (entity.getSellerResponseDeadline() != null && now.isAfter(entity.getSellerResponseDeadline())) {
                                    throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.GONE, "ACCEPTANCE_WINDOW_EXPIRED",
                                            "Seller response deadline has passed", java.util.Map.of("orderId", entity.getId()));
                                }
                                entity.setSellerAcceptedAt(java.time.Instant.now());
                                // Inventory hook (placeholder)
                                try { inventoryService.reserveForOrder(entity.getTenantId(), entity.getId()); } catch (Exception ex) { }
                            }
                            break;
                        case "preparing":
                            valid = "accepted".equals(cur);
                            break;
                        case "ready":
                            valid = "preparing".equals(cur);
                            break;
                        case "shipped":
                            valid = "ready".equals(cur);
                            break;
                        case "delivered":
                            valid = "shipped".equals(cur);
                            break;
                        case "rejected":
                            valid = "placed".equals(cur);
                            entity.setCancelledAt(java.time.Instant.now());
                            if (notes != null && !notes.isBlank()) entity.setCancellationReason(notes);
                            // Enforce SLA for reject as well
                            java.time.Instant now = java.time.Instant.now();
                            if (entity.getSellerResponseDeadline() != null && now.isAfter(entity.getSellerResponseDeadline())) {
                                throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.GONE, "ACCEPTANCE_WINDOW_EXPIRED",
                                        "Seller response deadline has passed", java.util.Map.of("orderId", entity.getId()));
                            }
                            entity.setSellerRejectedAt(java.time.Instant.now());
                            break;
                        case "cancelled":
                            // Guard: cancellation not allowed once READY/SHIPPED/DELIVERED
                            if ("ready".equalsIgnoreCase(cur) || "shipped".equalsIgnoreCase(cur) || "delivered".equalsIgnoreCase(cur)) {
                                throw new com.bharatshop.error.ApiException(
                                        org.springframework.http.HttpStatus.CONFLICT,
                                        "CANCEL_NOT_ALLOWED",
                                        "Order cannot be cancelled after it is READY",
                                        null
                                );
                            }
                            // allow seller cancel only before shipment lifecycle
                            valid = "placed".equals(cur) || "accepted".equals(cur) || "preparing".equals(cur);
                            entity.setCancelledAt(java.time.Instant.now());
                            if (notes != null && !notes.isBlank()) entity.setCancellationReason(notes);
                            break;
                        default:
                            // disallow unknown transitions to prevent legacy drift
                            valid = false;
                    }

                    if (valid) {
                        entity.setStatus(status);
                        orderRepository.save(entity);
                        // Release inventory when cancelled or rejected
                        if ("cancelled".equalsIgnoreCase(desired) || "rejected".equalsIgnoreCase(desired)) {
                            String tenantId = entity.getTenantId();
                            List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(entity.getId());
                            for (com.bharatshop.entity.OrderItemEntity oi : items) {
                                if (oi.getProductId() != null) {
                                    inventoryService.release(tenantId, oi.getProductId(), oi.getQuantity());
                                }
                            }
                        }
                        // Notify buyer on status change
                        try {
                            notificationService.sendOrderNotification(entity.getTenantId(), entity.getUserId(), entity.getId(), status.toUpperCase(),
                                    notes != null ? Map.of("notes", notes) : null);
                        } catch (Exception ex) { }
                    }
                    return DefaultSellerFactory.this.toDto(entity);
                }).orElse(null);
            }

            @Override
            public Order updateItemStatus(String orderId, String itemId, String status) {
                var orderOpt = orderRepository.findById(orderId);
                if (orderOpt.isEmpty()) return null;
                List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(orderId);
                com.bharatshop.entity.OrderItemEntity target = null;
                for (com.bharatshop.entity.OrderItemEntity it : items) {
                    if (it.getId().equals(itemId)) { target = it; break; }
                }
                if (target == null) return null;
                target.setStatus(status);
                target.setUpdatedAt(java.time.Instant.now());
                orderItemRepository.save(target);
                return DefaultSellerFactory.this.toDto(orderOpt.get());
            }

            @Override
            public Map<String, Object> refund(String orderId, Double amount, String reason) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("orderId", orderId);
                resp.put("amount", amount);
                resp.put("reason", reason);
                resp.put("status", "initiated");
                return resp;
            }
        };
    }





    @Override
    public SellerStoreOps stores() {
        return new SellerStoreOps() {
            @Override
            public List<Store> list(String search, Integer page, Integer limit) {
                var principal = com.bharatshop.security.UserPrincipal.current();
                if (principal == null) return java.util.Collections.emptyList();
                String ownerId = principal.getUserId();
                List<com.bharatshop.entity.StoreEntity> owned = storeRepository.findByOwnerId(ownerId);
                if (search != null && !search.isBlank()) {
                    String q = search.trim().toLowerCase();
                    owned = owned.stream().filter(e -> e.getName() != null && e.getName().toLowerCase().contains(q)).collect(Collectors.toList());
                }
                List<Store> all = owned.stream().map(e -> {
                    Store s = new Store(e.getId(), e.getName(), e.getArea(), e.getCategory());
                    s.setOwnerId(e.getOwnerId());
                    s.setOwnerPhone(e.getOwnerPhone());
                    s.setStatus(e.getStatus());
                    s.setOrderingDisabled(e.getOrderingDisabled());
                    s.setClosedReason(e.getClosedReason());
                    s.setClosedUntil(e.getClosedUntil());
                    s.setLogo(e.getLogo());
                    s.setUpdatedAt(e.getUpdatedAt());
                    return s;
                }).sorted(Comparator.comparing(Store::getName)).collect(Collectors.toList());
                if (limit != null && limit > 0) {
                    int p = page == null || page < 1 ? 1 : page;
                    int from = (p - 1) * limit;
                    if (from < all.size()) all = all.subList(from, Math.min(from + limit, all.size()));
                    else all = java.util.Collections.emptyList();
                }
                return all;
            }

            @Override
            public Store get(String id) {
                return storeService.get(id);
            }

            @Override
            public Store create(Store store) {
                if (store.getId() == null || store.getId().isBlank()) {
                    store.setId(java.util.UUID.randomUUID().toString());
                }
                return storeService.add(store);
            }

            @Override
            public Store updatePartial(String id, java.util.Map<String, Object> changes) {
                return storeRepository.findById(id).map(entity -> {
                    if (changes.containsKey("name")) entity.setName((String) changes.get("name"));
                    if (changes.containsKey("area")) entity.setArea((String) changes.get("area"));
                    if (changes.containsKey("city")) entity.setArea((String) changes.get("city"));
                    if (changes.containsKey("category")) entity.setCategory((String) changes.get("category"));
                    if (changes.containsKey("status")) entity.setStatus((String) changes.get("status"));
                    if (changes.containsKey("orderingDisabled")) {
                        Object v = changes.get("orderingDisabled");
                        if (v instanceof Boolean) entity.setOrderingDisabled((Boolean) v);
                        else if (v instanceof String) entity.setOrderingDisabled(Boolean.parseBoolean((String) v));
                    }
                    if (changes.containsKey("closedReason")) entity.setClosedReason((String) changes.get("closedReason"));
                    if (changes.containsKey("closedUntil")) {
                        Object cu = changes.get("closedUntil");
                        if (cu instanceof String && !((String) cu).isBlank()) {
                            try { entity.setClosedUntil(Instant.parse((String) cu)); } catch (Exception ignored) {}
                        } else if (cu instanceof java.time.Instant) {
                            entity.setClosedUntil((java.time.Instant) cu);
                        }
                    }
                    if (changes.containsKey("logo")) entity.setLogo((String) changes.get("logo"));
                    entity.setUpdatedAt(Instant.now());
                    storeRepository.save(entity);
                    Store s = new Store(entity.getId(), entity.getName(), entity.getArea(), entity.getCategory());
                    s.setOwnerId(entity.getOwnerId());
                    s.setOwnerPhone(entity.getOwnerPhone());
                    s.setStatus(entity.getStatus());
                    s.setOrderingDisabled(entity.getOrderingDisabled());
                    s.setClosedReason(entity.getClosedReason());
                    s.setClosedUntil(entity.getClosedUntil());
                    s.setLogo(entity.getLogo());
                    s.setUpdatedAt(entity.getUpdatedAt());
                    return s;
                }).orElse(null);
            }
        };
    }





    private Order toDto(com.bharatshop.entity.OrderEntity e) {
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
        List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
        List<com.bharatshop.domain.CartItem> cartItems = items.stream().map(oi -> {
            com.bharatshop.domain.CartItem ci = new com.bharatshop.domain.CartItem();
            ci.setId(oi.getId());
            ci.setName(oi.getName());
            ci.setPrice(oi.getPrice());
            ci.setQuantity(oi.getQuantity());
            ci.setRequiresPrescription(oi.getRequiresPrescription());
            ci.setStatus(oi.getStatus());
            return ci;
        }).collect(Collectors.toList());
        o.setItems(cartItems);
        return o;
    }
}