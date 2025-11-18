package com.bharatshop.factory.ops;

import java.util.Map;

public interface SellerAnnouncementOps {
    Map<String, Object> post(String storeId, String message, String activeUntil);
}