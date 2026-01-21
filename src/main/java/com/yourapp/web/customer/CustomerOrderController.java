package com.yourapp.web.customer;

import com.yourapp.dto.CustomerOrderDto;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.error.UnauthorizedException;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.security.rbac.CustomerOnly;
import com.bharatshop.service.OrderService;
import com.bharatshop.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/orders")
@CustomerOnly
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<CustomerOrderDto> list(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null) throw new UnauthorizedException("No customer principal");
        String tenant = TenantContext.getRequiredTenant();
        return orderService.getOrdersForCustomer(user.getUserId(), tenant);
    }

    @GetMapping("/{id}")
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

    @PostMapping("/{id}/cancel")
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