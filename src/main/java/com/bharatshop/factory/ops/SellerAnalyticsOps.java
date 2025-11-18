package com.bharatshop.factory.ops;

import java.util.Map;

public interface SellerAnalyticsOps {
    Map<String, Object> overview(String storeId, String from, String to);
}