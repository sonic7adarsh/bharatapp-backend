package com.bharatshop.factory.ops;

import com.bharatshop.domain.CartItem;
import com.bharatshop.domain.Order;
import com.bharatshop.domain.BookingDetails;
import java.util.List;

public interface OrderOps {
    Order placeOrder(String userId, List<CartItem> items, Order.Totals totals, String paymentMethod, Order.PaymentInfo paymentInfo, String type, String storeId, String notes);
    List<Order> listOrders(String userId);
    List<Order> listBookings(String userId);
    Order placeBooking(String userId, BookingDetails booking, Order.Totals totals, String paymentMethod, Order.PaymentInfo paymentInfo, String storeId, String notes);
}