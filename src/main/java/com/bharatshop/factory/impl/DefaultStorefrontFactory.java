package com.bharatshop.factory.impl;

import com.bharatshop.domain.Product;
import com.bharatshop.factory.StorefrontFactory;
import com.bharatshop.factory.ops.*;
import com.bharatshop.service.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultStorefrontFactory implements StorefrontFactory {
    private final ProductService productService;
    private final StoreService storeService;
    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;

    public DefaultStorefrontFactory(ProductService productService,
                                    StoreService storeService,
                                    OrderService orderService,
                                    CartService cartService,
                                    PaymentService paymentService,
                                    AvailabilityService availabilityService,
                                    BookingService bookingService) {
        this.productService = productService;
        this.storeService = storeService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.paymentService = paymentService;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
    }

    @Override public ProductOps products() {
        return new ProductOps() {
            @Override public List<com.bharatshop.domain.Product> list(String category, String search) {
                return productService.getAll(category, search);
            }
            @Override public com.bharatshop.domain.Product get(String id) { return productService.getById(id); }
            @Override public List<String> categories() { return productService.categories(); }
            @Override public void add(Product p) { productService.add(p); }
            @Override public List<Product> byStore(String storeId) { return productService.getByStore(storeId); }
        };
    }

    @Override public StoreOps stores() {
        return new StoreOps() {
            @Override public List<com.bharatshop.domain.Store> list(String search, String category) { return storeService.list(search, category); }
            @Override public com.bharatshop.domain.Store get(String id) { return storeService.get(id); }
            @Override public com.bharatshop.domain.Store add(com.bharatshop.domain.Store s) { return storeService.add(s); }
        };
    }

    @Override public OrderOps orders() {
        return new OrderOps() {
            @Override public com.bharatshop.domain.Order placeOrder(String userId, List<com.bharatshop.domain.CartItem> items, com.bharatshop.domain.Order.Totals totals, String paymentMethod, com.bharatshop.domain.Order.PaymentInfo paymentInfo, String type, String storeId, String notes) {
                return orderService.placeOrder(userId, items, totals, paymentMethod, paymentInfo, type, storeId, notes);
            }
            @Override public List<com.bharatshop.domain.Order> listOrders(String userId) { return orderService.listOrders(userId); }
            @Override public List<com.bharatshop.domain.Order> listBookings(String userId) { return orderService.listBookings(userId); }
            @Override public com.bharatshop.domain.Order placeBooking(String userId, com.bharatshop.domain.BookingDetails booking, com.bharatshop.domain.Order.Totals totals, String paymentMethod, com.bharatshop.domain.Order.PaymentInfo paymentInfo, String storeId, String notes) {
                return bookingService.placeBooking(userId, booking, totals, paymentMethod, paymentInfo, storeId, notes);
            }
        };
    }

    @Override public CartOps carts() {
        return new CartOps() {
            @Override public List<com.bharatshop.domain.CartItem> getCart(String userId) { return cartService.getCart(userId); }
            @Override public List<com.bharatshop.domain.CartItem> addItem(String userId, com.bharatshop.domain.CartItem item) { return cartService.addItem(userId, item); }
            @Override public List<com.bharatshop.domain.CartItem> removeItem(String userId, String itemId) { return cartService.removeItem(userId, itemId); }
        };
    }

    @Override public PaymentOps payments() {
        return new PaymentOps() {
            @Override public com.bharatshop.domain.PaymentOrder createOrder(int amount, String currency) { return paymentService.createOrder(amount, currency); }
            @Override public com.bharatshop.domain.PaymentVerificationResponse verify(String orderId, String paymentId, String signature) { return paymentService.verify(orderId, paymentId, signature); }
        };
    }

    @Override public AvailabilityOps availability() {
        return new AvailabilityOps() {
            @Override public com.bharatshop.domain.AvailabilityResponse check(String storeId, String roomId, String checkIn, String checkOut, int guests) {
                return availabilityService.check(storeId, roomId, checkIn, checkOut, guests);
            }
        };
    }
}