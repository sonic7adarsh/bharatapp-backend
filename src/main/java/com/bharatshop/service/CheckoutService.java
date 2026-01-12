package com.bharatshop.service;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.Store;
import com.bharatshop.dto.CheckoutRequest;
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
import com.bharatshop.error.ApiException;

@Service
public class CheckoutService {
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private final FactoryProvider factoryProvider;
    private final StoreAvailabilityPolicy storeAvailabilityPolicy;

    public CheckoutService(FactoryProvider factoryProvider, StoreAvailabilityPolicy storeAvailabilityPolicy) {
        this.factoryProvider = factoryProvider;
        this.storeAvailabilityPolicy = storeAvailabilityPolicy;
    }

    public ResponseEntity<?> checkout(CheckoutRequest req) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");

        String tenant = TenantContext.getTenant();
        log.info("CheckoutService: tenant={} userId={} storeId={}", tenant, up.getUserId(), req.getStoreId());

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BadRequestException("items must not be null or empty");
        }

        if (req.getStoreId() != null && !req.getStoreId().isBlank()) {
            Store store = factoryProvider.getFactory(tenant).stores().get(req.getStoreId());
            if (store == null) {
                throw new BadRequestException("Invalid storeId");
            }
            
            // Require delivery coordinates when address is provided for zone validation
            Double deliveryLat = req.getDeliveryLat();
            Double deliveryLng = req.getDeliveryLng();
            if (req.getAddress() != null && (deliveryLat == null || deliveryLng == null)) {
                throw new BadRequestException("delivery coordinates (lat,lng) are required");
            }
            
            // Enhanced availability check with inventory and zone validation
            Map<String, Object> availabilityError = storeAvailabilityPolicy.availabilityError(store, req.getItems(), deliveryLat, deliveryLng);
            if (availabilityError != null) {
                String code = String.valueOf(availabilityError.getOrDefault("code", "CONFLICT"));
                String message = String.valueOf(availabilityError.getOrDefault("message", "Conflict"));
                throw new ApiException(HttpStatus.CONFLICT, code, message, availabilityError);
            }
        }

        Order order = factoryProvider.getFactory(tenant).orders()
                .placeOrder(up.getUserId(), req.getItems(), req.getTotals(), req.getPaymentMethod(), req.getPaymentInfo(), "order", req.getStoreId(), req.getNotes());

        order.setAddress(req.getAddress());
        order.setDeliverySlot(req.getDeliverySlot());
        order.setDeliveryInstructions(req.getDeliveryInstructions());
        log.info("CheckoutService: success orderId={} reference={} userId={}", order.getId(), order.getReference(), up.getUserId());
        return ResponseEntity.ok(Map.of("order", order, "reference", order.getReference()));
    }
}