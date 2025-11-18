package com.bharatshop.service;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.CartItem;
import com.bharatshop.entity.OrderEntity;
import com.bharatshop.entity.OrderItemEntity;
import com.bharatshop.repository.OrderItemRepository;
import com.bharatshop.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingService bookingService;
    @org.springframework.beans.factory.annotation.Value("${app.orders.acceptanceWindowMinutes:15}")
    private int acceptanceWindowMinutes;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, BookingService bookingService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.bookingService = bookingService;
    }

    public Order placeOrder(String userId, List<CartItem> items, Order.Totals totals, String paymentMethod, Order.PaymentInfo paymentInfo, String type, String storeId, String notes) {
        // If this is a booking, delegate to BookingService and return a booking-shaped Order DTO
        if ("room_booking".equalsIgnoreCase(type)) {
            com.bharatshop.domain.BookingDetails booking = null;
            // In legacy flows, booking details come via totals or paymentInfo; here we assume controller passes booking via Order object later.
            // Since OrderService doesn't receive booking directly, callers like StoreLegacyController set booking on DTO after place.
            // We will create a minimal booking using notes and totals if needed.
            // For correctness, prefer StoreLegacyController to pass booking via CheckoutRequest; we handle that in factory/controller.
            // To avoid losing data, return a booking DTO persisted with minimal fields.
            return bookingService.placeBooking(userId, booking, totals, paymentMethod, paymentInfo, storeId, notes);
        }
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
                oi.setName(ci.getName());
                oi.setPrice(ci.getPrice());
                oi.setQuantity(ci.getQuantity());
                oi.setRequiresPrescription(ci.getRequiresPrescription());
                persist.add(oi);
            }
            orderItemRepository.saveAll(persist);
        }

        Order dto = toDto(e);
        dto.setItems(items);
        dto.setTotals(totals);
        dto.setPaymentInfo(paymentInfo);
        return dto;
    }

    public List<Order> listOrders(String userId) {
        List<OrderEntity> entities = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Instant now = Instant.now();
        for (OrderEntity e : entities) {
            if ("placed".equalsIgnoreCase(e.getStatus()) && e.getSellerAcceptedAt() == null && e.getSellerResponseDeadline() != null && now.isAfter(e.getSellerResponseDeadline())) {
                e.setStatus("cancelled");
                e.setCancelledAt(now);
                if (e.getCancellationReason() == null || e.getCancellationReason().isBlank()) {
                    e.setCancellationReason("auto_cancelled_no_response");
                }
                orderRepository.save(e);
            }
        }
        return entities.stream().map(this::toDtoWithItems).collect(Collectors.toList());
    }

    public List<Order> listBookings(String userId) {
        // Decoupled bookings: read from bookings repository/service
        return bookingService.listBookings(userId);
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
        ci.setId(oi.getId());
        ci.setName(oi.getName());
        ci.setPrice(oi.getPrice());
        ci.setQuantity(oi.getQuantity());
        ci.setRequiresPrescription(oi.getRequiresPrescription());
        return ci;
    }
}