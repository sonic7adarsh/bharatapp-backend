package com.bharatshop.factory.ops;

import com.bharatshop.domain.Product;
import java.util.List;
import java.util.Map;

public interface SellerProductOps {
    List<Product> listByStore(String storeId, String search, String category, Boolean active, Integer page, Integer limit);
    Product create(String storeId, Product product);
    Product updatePartial(String id, Map<String, Object> changes);
    boolean delete(String id, boolean archive);
    Product updateImage(String id, String filename);
    Product adjustInventory(String id, Integer stockDelta, Integer stockSet, Double price);
}