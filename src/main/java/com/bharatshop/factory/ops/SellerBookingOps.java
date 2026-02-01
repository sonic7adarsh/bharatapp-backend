package com.bharatshop.factory.ops;

import com.bharatshop.domain.Order;
import java.util.List;

public interface SellerBookingOps {
    List<Order> listBookings(String storeId, String status, String from, String to);
    Order updateStatus(String bookingId, String status, String notes);
}