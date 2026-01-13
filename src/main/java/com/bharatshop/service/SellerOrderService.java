package com.bharatshop.service;

import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.error.ApiException;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.OrderRepository;
import com.bharatshop.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SellerOrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;
    private final InventoryService inventoryService;

    public SellerOrderService(OrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              StoreRepository storeRepository,
                              InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.storeRepository = storeRepository;
        this.inventoryService = inventoryService;
    }

    public OrderEntity acceptOrder(String orderId, String sellerUserId) {
        OrderEntity e = findScoped(orderId);
        ensureOwnership(e, sellerUserId);
        ensureStatus(e, "placed", "ACCEPT_ONLY_FROM_PLACED");
        ensureDeadlineNotExpired(e, "ACCEPTANCE_WINDOW_EXPIRED");
        e.setStatus("accepted");
        e.setSellerAcceptedAt(Instant.now());
        orderRepository.save(e);
        // Inventory hook (placeholder only)
        inventoryService.reserveForOrder(e.getTenantId(), e.getId());
        return e;
    }

    public OrderEntity rejectOrder(String orderId, String sellerUserId, String reason) {
        OrderEntity e = findScoped(orderId);
        ensureOwnership(e, sellerUserId);
        ensureStatus(e, "placed", "REJECT_ONLY_FROM_PLACED");
        ensureDeadlineNotExpired(e, "ACCEPTANCE_WINDOW_EXPIRED");
        e.setStatus("rejected");
        e.setCancelledAt(Instant.now());
        e.setSellerRejectedAt(Instant.now());
        if (reason != null && !reason.isBlank()) e.setCancellationReason(reason);
        orderRepository.save(e);
        // Release reserved inventory on reject
        String tenantId = e.getTenantId();
        List<com.bharatshop.entity.OrderItemEntity> items = orderItemRepository.findByOrderId(e.getId());
        for (com.bharatshop.entity.OrderItemEntity oi : items) {
            if (oi.getProductId() != null) {
                inventoryService.release(tenantId, oi.getProductId(), oi.getQuantity());
            }
        }
        return e;
    }

    public OrderEntity markPreparing(String orderId, String sellerUserId) {
        OrderEntity e = findScoped(orderId);
        ensureOwnership(e, sellerUserId);
        ensureStatus(e, "accepted", "PREPARING_ONLY_FROM_ACCEPTED");
        e.setStatus("preparing");
        orderRepository.save(e);
        return e;
    }

    public OrderEntity markReady(String orderId, String sellerUserId) {
        OrderEntity e = findScoped(orderId);
        ensureOwnership(e, sellerUserId);
        ensureStatus(e, "preparing", "READY_ONLY_FROM_PREPARING");
        e.setStatus("ready");
        orderRepository.save(e);
        return e;
    }

    private OrderEntity findScoped(String orderId) {
        String tenant = com.bharatshop.tenant.TenantContext.getTenant();
        return (tenant != null && !tenant.isBlank() ?
                orderRepository.findByTenantIdAndId(tenant, orderId).orElse(null) :
                orderRepository.findById(orderId).orElse(null));
    }

    private void ensureOwnership(OrderEntity e, String sellerUserId) {
        if (e == null) throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found", null);
        if (e.getStoreId() == null) throw new ApiException(HttpStatus.FORBIDDEN, "STORE_UNKNOWN", "Order store unknown", null);
        StoreEntity store = storeRepository.findById(e.getStoreId()).orElse(null);
        if (store == null || store.getOwnerId() == null || !store.getOwnerId().equals(sellerUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_STORE_OWNER", "Seller does not own this store", null);
        }
    }

    private void ensureStatus(OrderEntity e, String expected, String code) {
        String cur = e.getStatus() == null ? "" : e.getStatus().toLowerCase();
        if (!expected.equalsIgnoreCase(cur)) {
            throw new ApiException(HttpStatus.CONFLICT, code, "Invalid order state for this action", java.util.Map.of("current", cur));
        }
    }

    private void ensureDeadlineNotExpired(OrderEntity e, String code) {
        Instant dl = e.getSellerResponseDeadline();
        if (dl != null && Instant.now().isAfter(dl)) {
            throw new ApiException(HttpStatus.GONE, code, "Seller response deadline has passed", java.util.Map.of("orderId", e.getId()));
        }
    }
}