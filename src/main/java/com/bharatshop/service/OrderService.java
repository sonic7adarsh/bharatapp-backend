package com.bharatshop.service;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.CartItem;
import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.OrderItemEntity;
import com.bharatshop.repository.OrderItemRepository;
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
    @org.springframework.beans.factory.annotation.Value("${app.orders.acceptanceWindowMinutes:15}")
    private int acceptanceWindowMinutes;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        InventoryService inventoryService, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
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
            List<Map.Entry<String, Integer>> reserved = new ArrayList<>();
            String tenantId = com.bharatshop.tenant.TenantContext.getTenant();
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

                // Attempt to reserve inventory atomically per item
                boolean ok = inventoryService.reserve(tenantId, ci.getId(), ci.getQuantity());
                if (!ok) {
                    // Rollback previously reserved items
                    for (Map.Entry<String, Integer> r : reserved) {
                        inventoryService.release(tenantId, r.getKey(), r.getValue());
                    }
                    // Cancel order and persist cancellation
                    e.setStatus("cancelled");
                    e.setCancelledAt(Instant.now());
                    e.setCancellationReason("inventory_unavailable");
                    orderRepository.save(e);

                    Map<String, Object> details = new HashMap<>();
                    details.put("productId", ci.getId());
                    details.put("requested", ci.getQuantity());
                    throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY",
                            "Insufficient stock for one or more items", details);
                }
                reserved.add(new AbstractMap.SimpleEntry<>(ci.getId(), ci.getQuantity()));
            }
            orderItemRepository.saveAll(persist);
        }

        // Send order placed notification
        try {
            notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "PLACED",
                    storeId != null ? Map.of("storeId", storeId) : null);
        } catch (Exception ex) {
            // Ignore notification errors to not block order placement
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
        }
        return entities.stream().map(this::toDtoWithItems).collect(Collectors.toList());
    }



    public Order cancelOrderByUser(String userId, String orderId, String reason) {
        return orderRepository.findById(orderId)
                .filter(e -> Objects.equals(e.getUserId(), userId))
                .map(e -> {
                    if ("delivered".equalsIgnoreCase(e.getStatus())) {
                        // Already delivered; cannot cancel
                        return toDtoWithItems(e);
                    }
                    e.setStatus("cancelled");
                    e.setCancelledAt(Instant.now());
                    if (reason != null && !reason.isBlank()) { e.setCancellationReason(reason); }
                    orderRepository.save(e);
                    // Release reserved inventory on cancellation
                    String tenantId = e.getTenantId();
                    List<OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
                    for (OrderItemEntity oi : items) {
                        if (oi.getProductId() != null) {
                            inventoryService.release(tenantId, oi.getProductId(), oi.getQuantity());
                        }
                    }
                    // Send order cancelled notification
                    try {
                        notificationService.sendOrderNotification(e.getTenantId(), e.getUserId(), e.getId(), "CANCELLED",
                                reason != null ? Map.of("reason", reason) : null);
                    } catch (Exception ex) {
                        // Ignore notification errors
                    }
                    return toDtoWithItems(e);
                })
                .orElse(null);
    }

    private Order toDto(OrderEntity e) {
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