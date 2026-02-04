package com.bharatshop.service;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.CartItem;
import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.entity.OrderItemEntity;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.repository.UserRepository;
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
    private final UserRepository userRepository;
    // private final TenantConfigurationService tenantConfigurationService; // Removed
    private final PaymentService paymentService;

    @org.springframework.beans.factory.annotation.Value("${app.orders.acceptanceWindowMinutes:15}")
    private int acceptanceWindowMinutes;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        InventoryService inventoryService, NotificationService notificationService,
                        StoreRepository storeRepository, UserRepository userRepository,
                        // TenantConfigurationService tenantConfigurationService,
                        PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        // this.tenantConfigurationService = tenantConfigurationService;
        this.paymentService = paymentService;
    }

    public Order placeOrder(String userId, List<CartItem> items, Order.Totals totals, String paymentMethod, Order.PaymentInfo paymentInfo, String type, String storeId, String notes, String prescriptionUrl, String deliveryAddress, String customerName, String customerPhone, String customerAlternatePhone) {
        String id = UUID.randomUUID().toString();
        OrderEntity e = new OrderEntity();
        e.setId(id);
        e.setReference("REF-" + id.substring(0, 8));
        e.setUserId(userId);
        e.setStatus("placed");
        e.setPaymentMethod(paymentMethod);
        e.setType(type == null ? "order" : type);
        e.setCreatedAt(Instant.now());
        e.setStoreId(storeId);
        e.setPrescriptionUrl(prescriptionUrl);
        e.setIsPrescriptionVerified(false);
        e.setDeliveryAddress(deliveryAddress);
        e.setCustomerName(customerName);
        e.setCustomerPhone(customerPhone);
        e.setCustomerAlternatePhone(customerAlternatePhone);
        if (storeId != null) {
            storeRepository.findById(storeId).ifPresent(store -> e.setSellerId(store.getOwnerId()));
        }
        e.setNotes(notes);
        if (acceptanceWindowMinutes > 0) {
            e.setSellerResponseDeadline(Instant.now().plusSeconds(acceptanceWindowMinutes * 60L));
        }
        double total = totals != null && totals.payable != null ? totals.payable :
                items == null ? 0 : items.stream().mapToDouble(ci -> ci.getPrice() * ci.getQuantity()).sum();
        e.setTotal(total);
        orderRepository.save(e);

        // Link payment if exists
        if (paymentInfo != null && paymentInfo.orderId != null && !paymentInfo.orderId.isBlank()) {
             try {
                 paymentService.linkOrder(paymentInfo.orderId, e.getId());
             } catch (Exception ex) {
                 // Log error
                 System.err.println("Failed to link payment " + paymentInfo.orderId + " to order " + e.getId() + ": " + ex.getMessage());
             }
        }

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

        // Atomic reserve at checkout: SKIPPED for MVP-1. Inventory managed manually by seller.
        // if (items != null && !items.isEmpty()) { ... }

        // Send order placed notification (buyer)
        try {
            Order dto = toDto(e);
            notificationService.sendLifecycleEvent(com.bharatshop.enums.NotificationEventType.ORDER_PLACED, dto, 
                    storeId != null ? Map.of("storeId", storeId) : null);
        } catch (Exception ex) {
            // Ignore notification errors to not block order placement
        }

        // Notify seller of new order
        try {
            if (storeId != null) {
                java.util.Optional<StoreEntity> storeOpt = storeRepository.findById(storeId);
                if (storeOpt.isPresent() && storeOpt.get().getOwnerId() != null) {
                    Order dto = toDto(e);
                    notificationService.sendLifecycleEvent(com.bharatshop.enums.NotificationEventType.NEW_ORDER_RECEIVED, dto,
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
        // Tenant context removed for local-first platform
        List<OrderEntity> entities = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
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
                        inventoryService.release(oi.getProductId(), oi.getQuantity());
                    }
                }
                // Notify buyer
                try {
                    notificationService.sendLifecycleEvent(com.bharatshop.enums.NotificationEventType.ORDER_CANCELLED, toDto(e), Map.of("reason", "auto_cancelled_no_response"));
                } catch (Exception ex) { }
            }

            // Enforce reservation TTL: SKIPPED for MVP-1 (manual inventory)
            /*
            if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null) {
                // Use default tenant or store-specific config if available
                int ttlMinutes = 15; // default
                Instant expiry = e.getCreatedAt() != null ? e.getCreatedAt().plusSeconds(ttlMinutes * 60L) : null;
                if (expiry != null && now.isAfter(expiry)) {
                    e.setStatus("cancelled");
                    e.setCancelledAt(now);
                    e.setCancellationReason("auto_cancelled_reservation_timeout");
                    orderRepository.save(e);
                    // Release reserved inventory
                    // SKIPPED
                    // Notify buyer
                    try {
                        notificationService.sendLifecycleEvent(com.bharatshop.enums.NotificationEventType.ORDER_CANCELLED, toDto(e), Map.of("reason", "auto_cancelled_reservation_timeout"));
                    } catch (Exception ex) { }
                }
            }
            */
        }
        return entities.stream().map(this::mapToDtoWithItems).collect(Collectors.toList());
    }

    public Order cancelOrderByAdmin(String orderId, String reason) {
        // tenantId parameter removed for local-first platform
        java.util.Optional<OrderEntity> opt = orderRepository.findById(orderId);
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

        // Process Refund
        try {
            paymentService.processRefundForShopOrder(e.getId(), reason != null ? reason : "Admin Cancelled");
        } catch (Exception ex) {
            System.err.println("Refund failed for order " + e.getId() + ": " + ex.getMessage());
        }

        // Release reserved inventory on cancel: SKIPPED for MVP-1
        // List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
        // for (OrderItemEntity oi : items) {
        //    if (oi.getProductId() != null) {
        //        inventoryService.release(oi.getProductId(), oi.getQuantity());
        //    }
        // }

        // Notify buyer and seller
        try {
            notificationService.sendLifecycleEvent(com.bharatshop.enums.NotificationEventType.ORDER_CANCELLED, toDto(e),
                    reason != null ? Map.of("reason", reason) : null);
        } catch (Exception ignore) {}

        return toDto(e);
    }



    public Order cancelOrderByUser(String userId, String orderId, String reason) {
        // Tenant context removed
        java.util.Optional<OrderEntity> opt = orderRepository.findById(orderId);
        return opt
                .filter(e -> Objects.equals(e.getUserId(), userId))
                .map(e -> {
                    String cur = e.getStatus() == null ? "" : e.getStatus().toUpperCase();
                    if ("READY".equals(cur) || "SHIPPED".equals(cur) || "DELIVERED".equals(cur)) {
                        throw new ApiException(HttpStatus.CONFLICT, "CANCEL_NOT_ALLOWED", "Cancellation not allowed after READY");
                    }
                    if ("CANCELLED".equals(cur)) {
                        return mapToDtoWithItems(e);
                    }
                    e.setStatus("cancelled");
                    e.setCancelledAt(Instant.now());
                    if (reason != null && !reason.isBlank()) { e.setCancellationReason(reason); }
                    orderRepository.save(e);
                    
                    // Process Refund
                    try {
                        paymentService.processRefundForShopOrder(e.getId(), reason != null ? reason : "User Cancelled");
                    } catch (Exception ex) {
                        System.err.println("Refund failed for order " + e.getId() + ": " + ex.getMessage());
                    }

                    // Release reserved inventory on cancellation: SKIPPED for MVP-1
                    // List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
                    // for (OrderItemEntity oi : items) {
                    //    if (oi.getProductId() != null) {
                    //        inventoryService.release(oi.getProductId(), oi.getQuantity());
                    //    }
                    // }
                    // Send order cancelled notification
                    try {
                        notificationService.sendLifecycleEvent(com.bharatshop.enums.NotificationEventType.ORDER_CANCELLED, mapToDtoWithItems(e),
                                reason != null ? Map.of("reason", reason) : null);
                    } catch (Exception ex) { }
                    return mapToDtoWithItems(e);
                })
                .orElse(null);
    }

    public java.util.Optional<com.yourapp.dto.CustomerOrderDto> getOrderForCustomer(String orderId, String userId) {
        return getOrderForCustomer(orderId, userId, null, null);
    }

    public java.util.Optional<com.yourapp.dto.CustomerOrderDto> getOrderForCustomer(String orderId, String userId, Double lat, Double lng) {
        java.util.Optional<OrderEntity> opt = orderRepository.findByIdAndUserId(orderId, userId);
        return opt.map(e -> toCustomerDtoWithItems(e, lat, lng));
    }

    public void cancelOrderForCustomer(String orderId, String userId) {
        java.util.Optional<OrderEntity> opt = orderRepository.findByIdAndUserId(orderId, userId);
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
              inventoryService.release(oi.getProductId(), oi.getQuantity());
         }
        }
        // Send order cancelled notification (buyer)
        try {
            notificationService.sendLifecycleEvent(com.bharatshop.enums.NotificationEventType.ORDER_CANCELLED, toDto(e), java.util.Map.of("reason", "customer_cancelled"));
        } catch (Exception ignore) {}
    }

    public java.util.List<com.yourapp.dto.CustomerOrderDto> getOrdersForCustomer(String userId) {
        return getOrdersForCustomer(userId, null, null);
    }

    public java.util.List<com.yourapp.dto.CustomerOrderDto> getOrdersForCustomer(String userId, Double lat, Double lng) {
        java.util.List<OrderEntity> entities = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return entities.stream().map(e -> toCustomerDtoWithItems(e, lat, lng)).collect(java.util.stream.Collectors.toList());
    }

    private com.yourapp.dto.CustomerOrderDto toCustomerDto(OrderEntity e, Double lat, Double lng) {
        com.yourapp.dto.CustomerOrderDto dto = new com.yourapp.dto.CustomerOrderDto();
        dto.setId(e.getId());
        dto.setStatus(e.getStatus());
        dto.setTotal(e.getTotal());
        dto.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        dto.setPaymentMethod(e.getPaymentMethod());
        dto.setType(e.getType());
        dto.setPrescriptionUrl(e.getPrescriptionUrl());
        dto.setIsPrescriptionVerified(e.getIsPrescriptionVerified());

        com.yourapp.dto.CustomerOrderDto.Store store = new com.yourapp.dto.CustomerOrderDto.Store();
        store.setId(e.getStoreId());
        try {
            if (e.getStoreId() != null) {
                java.util.Optional<com.bharatshop.entity.StoreEntity> s = storeRepository.findById(e.getStoreId());
                if (s.isPresent()) {
                    store.setName(s.get().getName());
                    // Populate seller contact
                    com.yourapp.dto.CustomerOrderDto.ContactInfo sellerInfo = new com.yourapp.dto.CustomerOrderDto.ContactInfo();
                    sellerInfo.setName(s.get().getName());
                    sellerInfo.setPhone(s.get().getOwnerPhone());
                    dto.setSellerContact(sellerInfo);
                    
                    com.yourapp.dto.CustomerOrderDto.Address addr = new com.yourapp.dto.CustomerOrderDto.Address();
                    addr.setFullAddress(s.get().getAddress());
                    dto.setAddress(addr);

                    // Calculate distance if coordinates provided
                    if (lat != null && lng != null && s.get().getLatitude() != null && s.get().getLongitude() != null) {
                        dto.setDistance(calculateDistance(lat, lng, s.get().getLatitude(), s.get().getLongitude()));
                    }
                }
            }
        } catch (Exception ignored) {}
        dto.setStore(store);

        // Populate customer contact
        com.yourapp.dto.CustomerOrderDto.ContactInfo custInfo = new com.yourapp.dto.CustomerOrderDto.ContactInfo();
        String name = e.getCustomerName();
        String phone = e.getCustomerPhone();
        
        // Always try to fetch fresh name from User table to avoid address label issues
        if (e.getUserId() != null) {
            var u = userRepository.findById(e.getUserId()).orElse(null);
            if (u != null) {
                name = u.getName();
                if (phone == null) phone = u.getPhone();
            }
        }
        
        custInfo.setName(name);
        custInfo.setPhone(phone);
        dto.setCustomerContact(custInfo);

        return dto;
    }

    private com.yourapp.dto.CustomerOrderDto toCustomerDtoWithItems(OrderEntity e) {
        return toCustomerDtoWithItems(e, null, null);
    }

    private com.yourapp.dto.CustomerOrderDto toCustomerDtoWithItems(OrderEntity e, Double lat, Double lng) {
        com.yourapp.dto.CustomerOrderDto dto = toCustomerDto(e, lat, lng);
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

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in km
    }

    public Order toDto(OrderEntity e) {
        Order o = new Order();
        // o.setId(e.getId()); // Removed for MVP-1
        // tenantId removed
        o.setUserId(e.getUserId());
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
        o.setPrescriptionUrl(e.getPrescriptionUrl());
        o.setIsPrescriptionVerified(e.getIsPrescriptionVerified());

        // Populate contact info
        if (e.getStoreId() != null) {
            storeRepository.findById(e.getStoreId()).ifPresent(s -> {
                Order.ContactInfo sellerInfo = new Order.ContactInfo();
                sellerInfo.name = s.getName();
                sellerInfo.phone = s.getOwnerPhone();
                sellerInfo.address = s.getAddress(); // Might be null but that's okay
                o.setSellerContact(sellerInfo);
            });
        }
        if (e.getUserId() != null) {
            Order.ContactInfo custInfo = new Order.ContactInfo();
            // Prefer snapshot from order entity
            if (e.getCustomerName() != null) {
                custInfo.name = e.getCustomerName();
                custInfo.phone = e.getCustomerPhone();
            } else {
                // Fallback to current user profile
                userRepository.findById(e.getUserId()).ifPresent(u -> {
                    custInfo.name = u.getName();
                    custInfo.phone = u.getPhone();
                });
            }
            o.setCustomerContact(custInfo);
            
            // Populate Address domain object
            if (e.getDeliveryAddress() != null) {
                com.bharatshop.domain.Address addr = new com.bharatshop.domain.Address();
                addr.setLine1(e.getDeliveryAddress());
                if (custInfo.name != null) addr.setName(custInfo.name);
                if (custInfo.phone != null) addr.setPhone(custInfo.phone);
                o.setAddress(addr);
            }
        }

        Order.Totals t = new Order.Totals();
        t.payable = e.getTotal();
        o.setTotals(t);
        return o;
    }

    public Order mapToDtoWithItems(OrderEntity e) {
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