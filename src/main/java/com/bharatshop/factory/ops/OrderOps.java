package com.bharatshop.factory.ops;

import com.bharatshop.domain.CartItem;
import com.bharatshop.domain.Order;
import java.util.List;

public interface OrderOps {
    Order placeOrder(String userId, List<CartItem> items, Order.Totals totals, String paymentMethod, Order.PaymentInfo paymentInfo, String type, String storeId, String notes);
    List<Order> listOrders(String userId);
    Order cancelOrder(String userId, String orderId, String reason);
}