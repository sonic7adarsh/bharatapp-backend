package com.bharatshop.factory;

import com.bharatshop.factory.ops.SellerAnnouncementOps;
import com.bharatshop.factory.ops.SellerAnalyticsOps;
import com.bharatshop.factory.ops.SellerBookingOps;
import com.bharatshop.factory.ops.SellerOrderOps;
import com.bharatshop.factory.ops.SellerProductOps;
import com.bharatshop.factory.ops.SellerStoreOps;
import com.bharatshop.factory.ops.SellerPayoutOps;

public interface SellerFactory {
    SellerProductOps products();
    SellerOrderOps orders();
    SellerBookingOps bookings();
    SellerAnalyticsOps analytics();
    SellerPayoutOps payouts();
    SellerAnnouncementOps announcements();
    SellerStoreOps stores();
}