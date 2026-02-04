package com.bharatshop.factory.impl;

import com.bharatshop.domain.Product;
import com.bharatshop.domain.Store;
import com.bharatshop.factory.StorefrontFactory;
import com.bharatshop.factory.ops.*;
import com.bharatshop.service.*;
import com.bharatshop.policy.StoreAvailabilityPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DefaultStorefrontFactory implements StorefrontFactory {
    private final ProductService productService;
    private final StoreService storeService;
    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final StoreAvailabilityPolicy storeAvailabilityPolicy;
    private final InventoryService inventoryService;
    private final CategoryService categoryService;
    
    public DefaultStorefrontFactory(ProductService productService,
                                    StoreService storeService,
                                    OrderService orderService,
                                    CartService cartService,
                                    PaymentService paymentService,
                                    StoreAvailabilityPolicy storeAvailabilityPolicy,
                                    InventoryService inventoryService,
                                    CategoryService categoryService) {
        this.productService = productService;
        this.storeService = storeService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.paymentService = paymentService;
        this.storeAvailabilityPolicy = storeAvailabilityPolicy;
        this.inventoryService = inventoryService;
        this.categoryService = categoryService;
    }

    @Override public ProductOps products() {
        return new ProductOps() {
            @Override public List<com.bharatshop.domain.Product> list(String category, String search) {
                return productService.getAll(category, search);
            }
            @Override public com.bharatshop.domain.Product get(String id) { return productService.getById(id); }
            @Override public List<String> categories() { 
                return categoryService.getCategories().stream()
                        .map(CategoryService.CategoryDto::getName)
                        .collect(java.util.stream.Collectors.toList());
            }
            @Override public void add(Product p) { productService.add(p); }
            @Override public List<Product> byStore(String storeId) { return productService.getActiveByStore(storeId); }
            @Override public List<Product> byStoreAndCategory(String storeId, String categoryId) { return productService.getByStoreAndCategory(storeId, categoryId); }
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
            @Override public com.bharatshop.domain.Order placeOrder(String userId, List<com.bharatshop.domain.CartItem> items, com.bharatshop.domain.Order.Totals totals, String paymentMethod, com.bharatshop.domain.Order.PaymentInfo paymentInfo, String type, String storeId, String notes, String prescriptionUrl, String deliveryAddress, String customerName, String customerPhone, String customerAlternatePhone) {
                return orderService.placeOrder(userId, items, totals, paymentMethod, paymentInfo, type, storeId, notes, prescriptionUrl, deliveryAddress, customerName, customerPhone, customerAlternatePhone);
            }
            @Override public List<com.bharatshop.domain.Order> listOrders(String userId) { return orderService.listOrders(userId); }

            @Override public com.bharatshop.domain.Order cancelOrder(String userId, String orderId, String reason) {
                return orderService.cancelOrderByUser(userId, orderId, reason);
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
            @Override
            public Map<String, Object> checkStoreAvailability(String storeId, Double lat, Double lng) {
                Store store = storeService.get(storeId);
                return storeAvailabilityPolicy.availabilityError(store, lat, lng);
            }
            
            @Override
            public Map<String, Object> checkInventoryAvailability(String storeId, String productId, int quantity) {
                boolean canReserve = inventoryService.canReserve(productId, quantity);
                if (!canReserve) {
                    int available = inventoryService.getAvailable(productId);
                    return Map.of(
                        "code", "INSUFFICIENT_INVENTORY",
                        "message", "Insufficient inventory",
                        "productId", productId,
                        "requested", quantity,
                        "available", available
                    );
                }
                return null;
            }
            
            @Override
            public Map<String, Object> checkDeliveryAvailability(String storeId, Double lat, Double lng) {
                Store store = storeService.get(storeId);
                return storeAvailabilityPolicy.checkZoneServiceability(store, lat, lng);
            }
        };
    }


}