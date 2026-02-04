package com.bharatshop.service;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.Product;
import com.bharatshop.domain.CartItem;
import com.bharatshop.domain.Store;
import com.bharatshop.dto.CheckoutRequest;
import com.bharatshop.dto.CheckoutItem;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.policy.StoreAvailabilityPolicy;
import com.bharatshop.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.error.BadRequestException;

import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.Duration;
import com.bharatshop.error.ApiException;

@Service
public class CheckoutService {
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private final FactoryProvider factoryProvider;
    private final StoreAvailabilityPolicy storeAvailabilityPolicy;
    private final com.bharatshop.repository.UserAddressRepository userAddressRepository;

    // Simple in-memory idempotency cache: key -> cached response and timestamp
    private static final ConcurrentHashMap<String, CacheEntry> IDEMPOTENCY_CACHE = new ConcurrentHashMap<>();
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private static class CacheEntry {
        final Map<String, Object> payload;
        final Instant createdAt;
        CacheEntry(Map<String, Object> payload) {
            this.payload = payload;
            this.createdAt = Instant.now();
        }
        boolean isExpired() { return Instant.now().isAfter(createdAt.plus(IDEMPOTENCY_TTL)); }
    }

    public CheckoutService(FactoryProvider factoryProvider, StoreAvailabilityPolicy storeAvailabilityPolicy, com.bharatshop.repository.UserAddressRepository userAddressRepository) {
        this.factoryProvider = factoryProvider;
        this.storeAvailabilityPolicy = storeAvailabilityPolicy;
        this.userAddressRepository = userAddressRepository;
    }

    public ResponseEntity<?> checkout(CheckoutRequest req, String idempotencyKey) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");

        log.info("CheckoutService: userId={}", up.getUserId());

        // If idempotency key provided and a valid cache exists, return cached result
        String cacheKey = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            cacheKey = String.join(":", up.getUserId(), idempotencyKey.trim());
            CacheEntry cached = IDEMPOTENCY_CACHE.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.info("CheckoutService: idempotent replay key={} -> returning cached order", idempotencyKey);
                return ResponseEntity.ok(cached.payload);
            } else if (cached != null && cached.isExpired()) {
                IDEMPOTENCY_CACHE.remove(cacheKey);
            }
        }

        // Log raw request BEFORE validation to expose mapping issues
        try {
            int size = req.getItems() != null ? req.getItems().size() : 0;
            String itemClass = size > 0 && req.getItems().get(0) != null ? req.getItems().get(0).getClass().getName() : "none";
            String productIds = req.getItems() != null ? req.getItems().stream().map(i -> {
                try { return i.getProductId(); } catch (Exception e) { return "<error>"; }
            }).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining(",")) : "";
            log.info("CheckoutRequest: items.size={} itemClass={} productIds={}", size, itemClass, productIds);
        } catch (Exception e) {
            log.warn("CheckoutRequest: logging error: {}", e.getMessage());
        }

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BadRequestException("items must not be null or empty");
        }
        // Derive storeId from first item’s product and enforce single-store rule
        String resolvedStoreId = null;
        java.util.List<CartItem> normalizedItems = new ArrayList<>();
        for (CheckoutItem itemReq : req.getItems()) {
            if (itemReq.getProductId() == null || itemReq.getProductId().isBlank()) {
                throw new BadRequestException("productId is required");
            }
            Product p = factoryProvider.getFactory().products().get(itemReq.getProductId());
            // HARD PROOF LOGS
            log.error("[PROOF][CHECKOUT] lookup productId={} found={}", itemReq.getProductId(), p != null);
            if (p != null) {
                log.error("[PROOF][CHECKOUT] product.id={} product.storeId={}",
                        p.getId(), p.getStoreId());
            }
            if (p == null) {
                throw new BadRequestException("Invalid productId");
            }
            String storeIdCandidate = p.getStoreId();
            if (storeIdCandidate == null) {
                throw new IllegalStateException("Product has no storeId");
            }
            if (resolvedStoreId == null) {
                resolvedStoreId = storeIdCandidate;
            } else if (!java.util.Objects.equals(resolvedStoreId, storeIdCandidate)) {
                throw new BadRequestException("Items must belong to a single store");
            }
            CartItem ci = new CartItem();
            ci.setId(p.getId());
            ci.setName(p.getName());
            ci.setPrice(p.getPrice());
            ci.setQuantity(itemReq.getQuantity());
            normalizedItems.add(ci);
        }
        if (resolvedStoreId == null) {
            throw new BadRequestException("Order storeId missing");
        }
        log.info("CheckoutService: storeId resolved={}", resolvedStoreId);
        // Enhanced availability check with inventory and zone validation using resolved storeId
        Store store = factoryProvider.getFactory().stores().get(resolvedStoreId);
        // MVP-1: Skip strict inventory availability check. Seller will verify manually.
        // We might still want to check if store is serviceable, which StoreAvailabilityPolicy can do if modified.
        // For now, assuming availabilityError returns null if we remove inventory checks there, or we skip it here.
        // Map<String, Object> availabilityError = storeAvailabilityPolicy.availabilityError(store, normalizedItems, null, null);
        // if (availabilityError != null) { ... }

        // Compute total from normalized items
        double computedTotal = normalizedItems.stream().mapToDouble(ci -> ci.getPrice() * ci.getQuantity()).sum();
        Order.Totals totals = new Order.Totals();
        totals.payable = computedTotal;
        log.info("CheckoutService: total computed={}", computedTotal);

        // Map Payment Info
        Order.PaymentInfo paymentInfo = null;
        if (req.getPaymentInfo() != null) {
            paymentInfo = new Order.PaymentInfo();
            paymentInfo.gateway = req.getPaymentInfo().gateway;
            paymentInfo.orderId = req.getPaymentInfo().orderId;
            paymentInfo.paymentId = req.getPaymentInfo().paymentId;
            // signature is verified in /verify endpoint, passing it here isn't strictly needed for linking but good for context if needed
        }

        // Resolve address and customer details
        String deliveryAddr = req.getDeliveryAddress();
        String custName = req.getCustomerName();
        String custPhone = req.getCustomerPhone();
        String custAltPhone = req.getCustomerAlternatePhone();

        if (req.getAddressId() != null) {
            com.bharatshop.entity.UserAddressEntity addr = userAddressRepository.findById(req.getAddressId()).orElse(null);
            if (addr != null && addr.getUserId().equals(up.getUserId())) {
                StringBuilder sb = new StringBuilder();
                if (addr.getLine1() != null) sb.append(addr.getLine1());
                if (addr.getLine2() != null) sb.append(", ").append(addr.getLine2());
                if (addr.getCity() != null) sb.append(", ").append(addr.getCity());
                if (addr.getState() != null) sb.append(", ").append(addr.getState());
                if (addr.getZip() != null) sb.append(" - ").append(addr.getZip());
                deliveryAddr = sb.toString();
                
                custName = up.getName(); // Use user's name instead of address label
                custPhone = addr.getPhone();
                custAltPhone = addr.getAlternatePhone();
            }
        }

        Order order = factoryProvider.getFactory().orders()
                .placeOrder(up.getUserId(), normalizedItems, totals, req.getPaymentMethod(), paymentInfo, "order", resolvedStoreId, null, req.getPrescriptionUrl(), deliveryAddr, custName, custPhone, custAltPhone);

        log.info("CheckoutService: success orderId={} reference={} userId={}", order.getId(), order.getReference(), up.getUserId());
        Map<String, Object> resp = Map.of("order", order, "reference", order.getReference());
        // Store in idempotency cache if key present
        if (cacheKey != null) {
            IDEMPOTENCY_CACHE.put(cacheKey, new CacheEntry(resp));
        }
        return ResponseEntity.ok(resp);
    }
}