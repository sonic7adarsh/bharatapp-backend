package com.bharatshop.customer;

import com.yourapp.dto.CustomerOrderDto;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.service.OrderService;
import com.bharatshop.service.CheckoutService;
import com.bharatshop.dto.CheckoutRequest;
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
        // If we have the Order, return the exact required contract
        if (order != null) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("order", order);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        // If checkout already produced a compliant payload, preserve it with 201
        if (body instanceof java.util.Map<?,?> m2 && m2.containsKey("order")) {
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }
        // Otherwise, return the original response (likely an error or alternative format)
        return r;
    }

    @GetMapping("/orders")
    public List<CustomerOrderDto> list(@AuthenticationPrincipal UserPrincipal user,
                                       @RequestParam(required = false) Double lat,
                                       @RequestParam(required = false) Double lng) {
        if (user == null) throw new UnauthorizedException("No customer principal");
        return orderService.getOrdersForCustomer(user.getUserId(), lat, lng);
    }

    @GetMapping("/orders/{id}")
    public CustomerOrderDto detail(
            @PathVariable String id,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        if (user == null) throw new UnauthorizedException("No customer principal");

        return orderService
                .getOrderForCustomer(id, user.getUserId(), lat, lng)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        if (user == null) throw new UnauthorizedException("No customer principal");

        orderService.cancelOrderForCustomer(id, user.getUserId());
        return ResponseEntity.noContent().build();
    }
}