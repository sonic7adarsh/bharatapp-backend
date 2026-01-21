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
import com.bharatshop.tenant.TenantContext;
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

    public CheckoutService(FactoryProvider factoryProvider, StoreAvailabilityPolicy storeAvailabilityPolicy) {
        this.factoryProvider = factoryProvider;
        this.storeAvailabilityPolicy = storeAvailabilityPolicy;
    }

    public ResponseEntity<?> checkout(CheckoutRequest req, String idempotencyKey) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");

        String tenant = TenantContext.getTenant();
        log.info("CheckoutService: tenant={} userId={}", tenant, up.getUserId());

        // If idempotency key provided and a valid cache exists, return cached result
        String cacheKey = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            cacheKey = String.join(":", tenant != null ? tenant : "", up.getUserId(), idempotencyKey.trim());
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
            Product p = factoryProvider.getFactory(tenant).products().get(itemReq.getProductId());
            // HARD PROOF LOGS
            log.error("[PROOF][CHECKOUT] lookup productId={} found={}", itemReq.getProductId(), p != null);
            if (p != null) {
                log.error("[PROOF][CHECKOUT] product.id={} product.storeId={} tenant={}",
                        p.getId(), p.getStoreId(), tenant);
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
        Store store = factoryProvider.getFactory(tenant).stores().get(resolvedStoreId);
        Map<String, Object> availabilityError = storeAvailabilityPolicy.availabilityError(store, normalizedItems, null, null);
        if (availabilityError != null) {
            String code = String.valueOf(availabilityError.getOrDefault("code", "CONFLICT"));
            String message = String.valueOf(availabilityError.getOrDefault("message", "Conflict"));
            throw new ApiException(HttpStatus.CONFLICT, code, message, availabilityError);
        }

        // Compute total from normalized items
        double computedTotal = normalizedItems.stream().mapToDouble(ci -> ci.getPrice() * ci.getQuantity()).sum();
        Order.Totals totals = new Order.Totals();
        totals.payable = computedTotal;
        log.info("CheckoutService: total computed={}", computedTotal);

        Order order = factoryProvider.getFactory(tenant).orders()
                .placeOrder(up.getUserId(), normalizedItems, totals, req.getPaymentMethod(), null, "order", resolvedStoreId, null);

        log.info("CheckoutService: success orderId={} reference={} userId={}", order.getId(), order.getReference(), up.getUserId());
        Map<String, Object> resp = Map.of("order", order, "reference", order.getReference());
        // Store in idempotency cache if key present
        if (cacheKey != null) {
            IDEMPOTENCY_CACHE.put(cacheKey, new CacheEntry(resp));
        }
        return ResponseEntity.ok(resp);
    }
}