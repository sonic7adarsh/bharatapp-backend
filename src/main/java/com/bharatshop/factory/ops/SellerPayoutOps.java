package com.bharatshop.factory.ops;

import java.util.List;
import java.util.Map;

public interface SellerPayoutOps {
    List<Map<String, Object>> list();
    Map<String, Object> request(double amount);
    Map<String, Object> getConfig();
    Map<String, Object> updateConfig(Map<String, Object> body);
}