package com.bharatshop.factory.ops;

import com.bharatshop.domain.Order;
import java.util.List;

public interface SellerOrderOps {
    List<Order> list(String storeId, String status, String from, String to, Integer page, Integer limit);
    Order get(String orderId);
    Order updateStatus(String orderId, String status, String notes);
    java.util.Map<String, Object> refund(String orderId, Double amount, String reason);
}