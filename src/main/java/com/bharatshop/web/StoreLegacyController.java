package com.bharatshop.web;

import com.bharatshop.domain.Order;
import com.bharatshop.domain.Product;
import com.bharatshop.dto.CheckoutRequest;
import com.bharatshop.factory.FactoryProvider;
import com.bharatshop.factory.StorefrontFactory;
import com.bharatshop.policy.StoreAvailabilityPolicy;
import com.bharatshop.tenant.TenantContext;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.error.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/store")
public class StoreLegacyController {
    private static final Logger log = LoggerFactory.getLogger(StoreLegacyController.class);
    private final FactoryProvider factoryProvider;
    private final StoreAvailabilityPolicy storeAvailabilityPolicy;

    public StoreLegacyController(FactoryProvider factoryProvider, StoreAvailabilityPolicy storeAvailabilityPolicy) {
        this.factoryProvider = factoryProvider;
        this.storeAvailabilityPolicy = storeAvailabilityPolicy;
    }

    private StorefrontFactory factory(String tenant) { return factoryProvider.getFactory(tenant); }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> listProducts(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                                      @RequestParam(required = false) String category,
                                                      @RequestParam(required = false) String search,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size,
                                                      @RequestParam(required = false) String sort) {
        // Pagination/sorting are ignored in this mock. Filtering supported.
        log.info("Legacy list products: tenant={} category={} search={}", tenant != null ? tenant : TenantContext.getTenant(), category, search);
        return ResponseEntity.ok(factory(tenant).products().list(category, search));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                        @PathVariable String id) {
        log.info("Legacy get product: tenant={} id={} ", tenant != null ? tenant : TenantContext.getTenant(), id);
        Product p = factory(tenant).products().get(id);
        if (p == null) throw new NotFoundException("Product not found");
        log.info("Legacy get product success: id={} name={}", p.getId(), p.getName());
        return ResponseEntity.ok(p);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        log.info("Legacy list categories: tenant={}", tenant != null ? tenant : TenantContext.getTenant());
        return ResponseEntity.ok(factory(tenant).products().categories());
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                      @Valid @RequestBody CheckoutRequest req) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");
        log.info("Legacy checkout: tenant={} userId={} items={} paymentMethod={}", tenant != null ? tenant : TenantContext.getTenant(), up.getUserId(),
                req.getItems() != null ? req.getItems().size() : 0, req.getPaymentMethod());
        // Enforce store availability
        if (req.getStoreId() != null && !req.getStoreId().isBlank()) {
            com.bharatshop.domain.Store store = factory(tenant).stores().get(req.getStoreId());
            if (store == null) throw new BadRequestException("Invalid storeId");
            java.util.Map<String, Object> availabilityError = storeAvailabilityPolicy.availabilityError(store);
            if (availabilityError != null) {
                String code = String.valueOf(availabilityError.getOrDefault("code", "CONFLICT"));
                String message = String.valueOf(availabilityError.getOrDefault("message", "Conflict"));
                throw new com.bharatshop.error.ApiException(HttpStatus.CONFLICT, code, message, availabilityError);
            }
        }
        Order order;
        String type = req.getType() == null ? "order" : req.getType();
        if ("room_booking".equalsIgnoreCase(type)) {
            order = factory(tenant).orders()
                    .placeBooking(up.getUserId(), req.getBooking(), req.getTotals(), req.getPaymentMethod(), req.getPaymentInfo(), req.getStoreId(), req.getNotes());
        } else {
            order = factory(tenant).orders()
                    .placeOrder(up.getUserId(), req.getItems(), req.getTotals(), req.getPaymentMethod(), req.getPaymentInfo(), req.getType(), req.getStoreId(), req.getNotes());
        }
        order.setAddress(req.getAddress());
        order.setDeliverySlot(req.getDeliverySlot());
        order.setDeliveryInstructions(req.getDeliveryInstructions());
        order.setPromo(req.getPromo());
        order.setBooking(req.getBooking());
        log.info("Legacy checkout success: orderId={} reference={} userId={}", order.getId(), order.getReference(), up.getUserId());
        return ResponseEntity.ok(Map.of("order", order, "reference", order.getReference()));
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");
        log.info("Legacy list orders: tenant={} userId={}", tenant != null ? tenant : TenantContext.getTenant(), up.getUserId());
        List<Order> orders = factory(tenant).orders().listOrders(up.getUserId());
        log.info("Legacy orders fetched: count={}", orders != null ? orders.size() : 0);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> orderDetail(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                         @PathVariable String id) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new UnauthorizedException("Unauthorized");
        log.info("Legacy order detail: tenant={} userId={} id={}", tenant != null ? tenant : TenantContext.getTenant(), up.getUserId(), id);
        List<Order> orders = factory(tenant).orders().listOrders(up.getUserId());
        Order match = null;
        for (Order o : orders) {
            if (id.equals(o.getId())) { match = o; break; }
        }
        if (match == null) throw new NotFoundException("Order not found");
        log.info("Legacy order detail success: id={} status={}", match.getId(), match.getStatus());
        return ResponseEntity.ok(match);
    }
}