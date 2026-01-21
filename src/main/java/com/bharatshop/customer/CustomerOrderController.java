package com.bharatshop.customer;

import com.yourapp.dto.CustomerOrderDto;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.service.OrderService;
import com.bharatshop.service.CheckoutService;
import com.bharatshop.dto.CheckoutRequest;
import com.bharatshop.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.bharatshop.domain.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;

    public CustomerOrderController(OrderService orderService, CheckoutService checkoutService) {
        this.orderService = orderService;
        this.checkoutService = checkoutService;
    }

    @PostMapping("/orders")
    public ResponseEntity<?> placeOrder(@RequestBody CheckoutRequest request,
                                        @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                        @AuthenticationPrincipal UserPrincipal user) {
        if (user == null) throw new UnauthorizedException("No customer principal");
        ResponseEntity<?> r = checkoutService.checkout(request, idempotencyKey);
        Object body = r.getBody();
        Order order = null;
        if (body instanceof java.util.Map<?,?> m) {
            Object o = m.get("order");
            if (o instanceof Order) order = (Order) o;
        } else if (body instanceof Order) {
            order = (Order) body;
        }
        if (order == null) {
            // Fallback: return original body
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        Double total = order.getTotal();
        if (total == null && order.getTotals() != null) {
            total = order.getTotals().payable;
        }
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("orderId", order.getId());
        resp.put("status", order.getStatus() != null ? order.getStatus().toUpperCase() : "PLACED");
        resp.put("totalAmount", total);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/orders")
    public List<CustomerOrderDto> list(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null) throw new UnauthorizedException("No customer principal");
        String tenant = TenantContext.getRequiredTenant();
        return orderService.getOrdersForCustomer(user.getUserId(), tenant);
    }

    @GetMapping("/orders/{id}")
    public CustomerOrderDto detail(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        if (user == null) throw new UnauthorizedException("No customer principal");
        String tenant = TenantContext.getRequiredTenant();

        return orderService
                .getOrderForCustomer(id, user.getUserId(), tenant)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        if (user == null) throw new UnauthorizedException("No customer principal");
        String tenant = TenantContext.getRequiredTenant();

        orderService.cancelOrderForCustomer(id, user.getUserId(), tenant);
        return ResponseEntity.noContent().build();
    }
}