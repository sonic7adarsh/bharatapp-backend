package com.bharatshop.factory.ops;

import com.bharatshop.domain.Order;
import com.bharatshop.dto.seller.SellerOrderDetail;
import com.bharatshop.dto.seller.SellerOrderSummary;
import java.util.List;

public interface SellerOrderOps {
    List<Order> list(String storeId, String status, String from, String to, Integer page, Integer limit);
    List<SellerOrderSummary> listSummaries(String storeId, String status, String from, String to, Integer page, Integer limit);
    Order get(String orderId);
    SellerOrderDetail getDetail(String orderId);
    Order updateStatus(String orderId, String status, String notes);
    Order updateItemStatus(String orderId, String itemId, String status);
    java.util.Map<String, Object> refund(String orderId, Double amount, String reason);
}
